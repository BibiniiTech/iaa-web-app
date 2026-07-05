'use client';

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { onAuthStateChanged, User } from 'firebase/auth';
import { doc, getDoc, addDoc, collection } from 'firebase/firestore';
import { ref, uploadBytes, getDownloadURL } from 'firebase/storage';
import { auth, db, storage } from '@/lib/firebase';
import styles from './submissions.module.css';
import FileUpload from '@/components/submissions/FileUpload';
import { REGIONS, MMDA_DATA } from '@/data/mmda_data';
import {
  getMdaTo,
  getMdaCc,
  getMmdaTo,
  getRegionCc,
  getMmdaInstitutionCc
} from '@/lib/email-utils';

const SubmissionIcons = {
  Quarterly: () => (
    <svg width="48" height="48" viewBox="0 0 24 24" fill="currentColor">
      <path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm4 18H6V4h7v5h5v11zM8 15.01l1.41 1.41L11 14.84V19h2v-4.16l1.59 1.59L16 15.01 12.01 11 8 15.01z"/>
    </svg>
  ),
  Annual: () => (
    <svg width="48" height="48" viewBox="0 0 24 24" fill="currentColor">
      <path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/>
    </svg>
  ),
  CCC: () => (
    <svg width="48" height="48" viewBox="0 0 24 24" fill="currentColor">
      <path d="M13 3c-4.97 0-9 4.03-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42C8.27 19.99 10.51 21 13 21c4.97 0 9-4.03 9-9s-4.03-9-9-9zm-1 5v5l4.28 2.54.72-1.21-3.5-2.08V8H12z"/>
    </svg>
  ),
  SOI: () => (
    <svg width="48" height="48" viewBox="0 0 24 24" fill="currentColor">
      <path d="M19 3h-4.18C14.4 1.84 13.3 1 12 1s-2.4.84-2.82 2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 0c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zm2 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z"/>
    </svg>
  ),
  Other: () => (
    <svg width="48" height="48" viewBox="0 0 24 24" fill="currentColor">
      <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
    </svg>
  )
};

type SubmissionType = 'soi' | 'quarterly' | 'annual' | 'ccc' | 'other' | null;

type Deadline = {
  report: string;
  dateString: string;
};

type PortalConfig = {
  description: string;
};

type SubmissionDraft = {
  institutionType?: string;
  institutionName?: string;
  region?: string;
  mmda?: string;
  period?: string;
  quarter?: string;
  reportType?: string;
};

const DEFAULT_DEADLINES: Deadline[] = [
  { report: "1st Quarter Internal Audit Report", dateString: "2026-04-30" },
  { report: "2nd Quarter Internal Audit Report", dateString: "2026-07-31" },
  { report: "3rd Quarter Internal Audit Report", dateString: "2026-10-31" },
  { report: "Status of Implementation on 4th Quarter IA Report", dateString: "2026-04-30" },
  { report: "Status of Implementation on 1st Quarter IA Report", dateString: "2026-07-31" },
  { report: "Status of Implementation on 2nd Quarter IA Report", dateString: "2026-10-31" },
  { report: "1st Quarter Commitment Control Compliance (CCC) Review Report", dateString: "2026-04-10" },
  { report: "2nd Quarter Commitment Control Compliance (CCC) Review Report", dateString: "2026-07-10" },
  { report: "3rd Quarter Commitment Control Compliance (CCC) Review Report", dateString: "2026-10-10" },
  { report: "2025 Annual Statement on Status of Audit Recommendations Implementations", dateString: "2026-06-30" }
];

export default function SubmissionsPage() {
  const [user, setUser] = useState<User | null>(null);
  const [activePortal, setActivePortal] = useState<SubmissionType>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [status, setStatus] = useState<{ type: 'success' | 'error', message: string } | null>(null);

  // Form State
  const [institutionType, setInstitutionType] = useState('MDA');
  const [institutionName, setInstitutionName] = useState('');
  const [region, setRegion] = useState('');
  const [mmda, setMmda] = useState('');
  const [period, setPeriod] = useState('');
  const [quarter, setQuarter] = useState('');
  const [reportType, setReportType] = useState('');
  const [senderName, setSenderName] = useState('');
  const [senderEmail, setSenderEmail] = useState('');
  const [files, setFiles] = useState<File[]>([]);
  const [deadlines, setDeadlines] = useState<Deadline[]>([]);
  const [portalConfig, setPortalConfig] = useState<PortalConfig>({
    description: 'Facilitating easy and reliable submission of reports and documentations to the IAA, with copies automatically sent to the relevant stakeholders.'
  });

  const router = useRouter();

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (currentUser) => {
      setUser(currentUser);
      if (!currentUser && !loading) {
        router.push('/login');
      }
    });
    return () => unsubscribe();
  }, [router, loading]);

  useEffect(() => {
    if (!user) return;

    const fetchData = async () => {
      try {
        // Fetch User Profile
        const userDoc = await getDoc(doc(db, 'users', user.uid));
        if (userDoc.exists()) {
          const userData = userDoc.data();
          setSenderName(`${userData.firstName || ''} ${userData.surname || ''}`.trim());
          setSenderEmail(String(userData.email || ''));
        }

        // Fetch Portal Config (welcomeNotice from config/portal)
        const portalSnap = await getDoc(doc(db, 'config', 'portal'));
        if (portalSnap.exists()) {
          const data = portalSnap.data();
          if (data.welcomeNotice) {
            setPortalConfig({ description: data.welcomeNotice });
          }
        }

        // Fetch Deadlines (json from config/deadlines)
        try {
          const deadlinesSnap = await getDoc(doc(db, 'config', 'deadlines'));
          let fetchedDeadlines: Deadline[] = [];

          if (deadlinesSnap.exists()) {
            const json = deadlinesSnap.data()?.json;
            if (json) {
              const parsed = JSON.parse(json);
              fetchedDeadlines = Array.isArray(parsed) ? parsed : [];
            }
          }

          // Use defaults if fetched list is empty (Exact Android Logic)
          const allDeadlines = fetchedDeadlines.length > 0 ? fetchedDeadlines : DEFAULT_DEADLINES;

          const today = new Date();
          const todayStr = today.toISOString().split('T')[0];

          // Filter out past deadlines and take only the next 5 (Exact Android Logic)
          const currentDeadlines = allDeadlines
            .filter((d) => d.dateString && d.dateString >= todayStr)
            .sort((a, b) => a.dateString.localeCompare(b.dateString))
            .slice(0, 5);

          setDeadlines(currentDeadlines);
        } catch (error) {
          console.error("Error fetching deadlines:", error);
        }
      } catch (error) {
        console.error("Error fetching submissions data:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [user]);

  useEffect(() => {
    if (!loading && window.location.hash === '#deadlines') {
      setTimeout(() => {
        const element = document.getElementById('deadlines');
        if (element) {
          element.scrollIntoView({ behavior: 'smooth' });
        }
      }, 100);
    }
  }, [loading]);

  const openPortal = (portal: Exclude<SubmissionType, null>) => {
    const savedDraft = localStorage.getItem(`draft_${portal}`);
    if (savedDraft) {
      const draft = JSON.parse(savedDraft) as SubmissionDraft;
      setInstitutionType(draft.institutionType || 'MDA');
      setInstitutionName(draft.institutionName || '');
      setRegion(draft.region || '');
      setMmda(draft.mmda || '');
      setPeriod(draft.period || '');
      setQuarter(draft.quarter || '');
      setReportType(draft.reportType || '');
    }
    setActivePortal(portal);
  };

  useEffect(() => {
    if (activePortal) {
      const draft = { institutionType, institutionName, region, mmda, period, quarter, reportType };
      localStorage.setItem(`draft_${activePortal}`, JSON.stringify(draft));
    }
  }, [institutionType, institutionName, region, mmda, period, quarter, reportType, activePortal]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) {
      setStatus({ type: 'error', message: 'Please sign in before submitting a report.' });
      return;
    }

    if (files.length === 0) {
      setStatus({ type: 'error', message: 'Please attach at least one file.' });
      return;
    }

    setSubmitting(true);
    setStatus(null);

    try {
      const institution = institutionType === 'MDA' ? institutionName.trim() : mmda.trim();
      const category = activePortal || 'other';

      // 1. Resolve Recipient Emails exactly like the Android App
      const toRecipients: string[] = [];
      const ccRecipients: string[] = [];

      if (institutionType === 'MDA') {
        toRecipients.push(...await getMdaTo(category));
        ccRecipients.push(...await getMdaCc(category));
      } else {
        toRecipients.push(...await getMmdaTo(category));
        ccRecipients.push(...await getRegionCc(category, region));
        if (category === 'annual' || category === 'quarterly') {
          ccRecipients.push(...await getMmdaInstitutionCc(category, region, institution));
        }
      }

      if (toRecipients.length === 0) {
        setStatus({ type: 'error', message: 'No recipient emails configured for this category.' });
        setSubmitting(false);
        return;
      }

      // 2. Upload Files to Firebase Storage
      const uploadedFileUrls: { name: string, url: string, storagePath: string }[] = [];
      const timestamp = Date.now();
      for (const file of files) {
        const safeFileName = file.name.replace(/[\\/]/g, '_');
        const storageName = `${timestamp}_${safeFileName}`;
        const storagePath = `submissions/${user.uid}/${category}/${storageName}`;
        const fileRef = ref(storage, storagePath);
        const snapshot = await uploadBytes(fileRef, file);
        const url = await getDownloadURL(snapshot.ref);
        uploadedFileUrls.push({ name: file.name, url, storagePath });
      }

      // 3. Format Subject and Body text matching the Android App screens
      let subject = '';
      let bodyText = '';

      if (category === 'quarterly') {
        subject = `${quarter} Quarter Internal Audit Report for ${institution}`;
        bodyText = `Kindly find attached the submission of the ${quarter} Quarter Internal Audit Report for ${institution}\n\nSent by ${senderName} ${senderEmail}`;
      } else if (category === 'annual') {
        subject = institutionType === 'MDA' ? `${reportType} for ${institution}` : `${reportType} Report for ${institution}`;
        bodyText = `Kindly find attached the submission of the ${reportType} for ${institution}\n\nSent by ${senderName} ${senderEmail}`;
      } else if (category === 'ccc') {
        subject = `${quarter} Quarter CCC Review Report for ${institution}`;
        bodyText = `Kindly find attached the submission of the ${quarter} Quarter CCC Review Report for ${institution}\n\nSent by ${senderName} ${senderEmail}`;
      } else if (category === 'soi') {
        subject = `${quarter} Quarter Status of Implementation on Audit Reports for ${institution}`;
        bodyText = `Kindly find attached the submission of the ${quarter} Quarter Quarter Status of Implementation on Audit Reports for ${institution}\n\nSent by ${senderName} ${senderEmail}`;
      } else {
        // 'other' report submission
        subject = `${period} from ${institution}`;
        bodyText = `Kindly find attached the submission of ${period} from ${institution}\n\nSent by ${senderName} ${senderEmail}`;
      }

      // Convert bodyText to HTML matching EmailSender.kt format
      const htmlContent = `<html><body>${bodyText.replace(/\n/g, '<br>')}</body></html>`;

      // 4. Log Submission in Firestore matching FirebaseHelper.logSubmission
      const displayPeriod = (category === 'soi' || category === 'quarterly' || category === 'ccc') ? `${quarter} Quarter` :
                           (category === 'annual') ? reportType : period;

      const reportName = (category === 'soi') ? `${quarter} Quarter SOI Report` :
                        (category === 'quarterly') ? `${quarter} Quarter Report` :
                        (category === 'ccc') ? `${quarter} Quarter CCC Review Report` :
                        (category === 'annual') ? reportType : period;

      await addDoc(collection(db, 'submissions'), {
        // Keeping Web Portal properties for backward compatibility
        userId: user.uid,
        type: category,
        period: displayPeriod,
        files: uploadedFileUrls.map(f => ({ name: f.name, url: f.url })),

        // Android Sync Schema properties
        category: category,
        institutionType: institutionType,
        institution: institution,
        region: region,
        senderName: senderName,
        senderEmail: senderEmail,
        timestamp: timestamp, // Use the same timestamp as the files
        fileNames: uploadedFileUrls.map(f => f.storagePath.split('/').pop() || f.name),
        filePaths: uploadedFileUrls.map(f => f.storagePath),
        reportName: reportName,
      });

      // 5. Send using the same Brevo setup (via api endpoint)
      const response = await fetch('/api/send-email', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          to: toRecipients.map((email: string) => ({ email })),
          cc: ccRecipients.length > 0 ? ccRecipients.map((email: string) => ({ email })) : undefined,
          subject: subject,
          htmlContent: htmlContent,
          attachments: uploadedFileUrls.map(f => ({ url: f.url, name: f.name })),
        }),
      });

      if (!response.ok) {
        throw new Error('Failed to send email notification');
      }

      setStatus({ type: 'success', message: 'Report submitted successfully!' });
      localStorage.removeItem(`draft_${activePortal}`);

      setTimeout(() => {
        setActivePortal(null);
        setFiles([]);
        setStatus(null);
      }, 3000);

    } catch (error: unknown) {
      console.error(error);
      const message = error instanceof Error ? error.message : 'An error occurred during submission.';
      setStatus({ type: 'error', message });
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <div className={styles.container}>Loading...</div>;

  if (!activePortal) {
    return (
      <div className={styles.container}>
        <h1 className={styles.title}>Submissions Portal</h1>
        <p className={styles.description}>
          {portalConfig.description}
        </p>

        <h3 className={styles.sectionTitle}>What would you like to submit?</h3>

        <div className={styles.portalGrid}>
          <div className={styles.portalCard} onClick={() => openPortal('quarterly')}>
            <span className={styles.cardIcon}><SubmissionIcons.Quarterly /></span>
            <span className={styles.cardTitle}>Quarterly IA Reports</span>
            <p className="text-muted">Standard quarterly submissions</p>
          </div>
          <div className={styles.portalCard} onClick={() => openPortal('annual')}>
            <span className={styles.cardIcon}><SubmissionIcons.Annual /></span>
            <span className={styles.cardTitle}>Annual Reports</span>
            <p className="text-muted">Annual audit planning documents</p>
          </div>
          <div className={styles.portalCard} onClick={() => openPortal('ccc')}>
            <span className={styles.cardIcon}><SubmissionIcons.CCC /></span>
            <span className={styles.cardTitle}>CCC Review Reports</span>
            <p className="text-muted">Quarterly CCC review submissions</p>
          </div>
          <div className={styles.portalCard} onClick={() => openPortal('soi')}>
            <span className={styles.cardIcon}><SubmissionIcons.SOI /></span>
            <span className={styles.cardTitle}>Status of Implementation</span>
            <p className="text-muted">Quarterly SOI on Audit Reports</p>
          </div>
          <div className={styles.portalCard} onClick={() => openPortal('other')}>
            <span className={styles.cardIcon}><SubmissionIcons.Other /></span>
            <span className={styles.cardTitle}>Other Reports</span>
            <p className="text-muted">Special audits and other documents</p>
          </div>
        </div>

        {deadlines.length > 0 && (
          <div id="deadlines" className={styles.deadlinesCard}>
            <h3 className={styles.deadlinesTitle}>Upcoming Submissions</h3>
            <div className={styles.deadlinesTableWrapper}>
              <table className={styles.deadlinesTable}>
                <thead>
                  <tr>
                    <th>Report Type</th>
                    <th>Deadline</th>
                  </tr>
                </thead>
                <tbody>
                  {deadlines.map((d, i) => (
                    <tr key={i}>
                      <td>{d.report}</td>
                      <td>{new Date(d.dateString).toLocaleDateString('en-US', { month: 'short', day: '2-digit', year: 'numeric' })}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    );
  }

  return (
    <div className={styles.container}>
      <div className={styles.formContainer}>
        <header className={styles.formTitle}>
          <button className={styles.backButton} onClick={() => setActivePortal(null)}>←</button>
          <h2>{activePortal === 'soi' ? 'Status of Implementation' :
               activePortal === 'quarterly' ? 'Quarterly IA Report' :
               activePortal === 'ccc' ? 'CCC Review Report' :
               activePortal === 'annual' ? 'Annual Report' : 'Other Report'} Submission</h2>
        </header>

        {status && (
          <div className={status.type === 'success' ? styles.success : styles.error}>
            {status.message}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className={styles.inputGroup}>
            <label>Institution Type</label>
            <select
              className={styles.selectField}
              value={institutionType}
              onChange={(e) => setInstitutionType(e.target.value)}
            >
              <option value="MDA">MDA</option>
              <option value="RCC/MMDA">RCC/MMDA</option>
            </select>
          </div>

          {institutionType === 'MDA' ? (
            <div className={styles.inputGroup}>
              <label>Institution Name</label>
              <input
                type="text"
                className={styles.inputField}
                value={institutionName}
                onChange={(e) => setInstitutionName(e.target.value)}
                required
              />
            </div>
          ) : (
            <>
              <div className={styles.inputGroup}>
                <label>Region</label>
                <select
                  className={styles.selectField}
                  value={region}
                  onChange={(e) => { setRegion(e.target.value); setMmda(''); }}
                  required
                >
                  <option value="">Select Region</option>
                  {REGIONS.map(r => <option key={r} value={r}>{r}</option>)}
                </select>
              </div>
              {region && (
                <div className={styles.inputGroup}>
                  <label>Institution (RCC/MMDA)</label>
                  <select
                    className={styles.selectField}
                    value={mmda}
                    onChange={(e) => setMmda(e.target.value)}
                    required
                  >
                    <option value="">Select Institution</option>
                    {MMDA_DATA[region]?.map(m => <option key={m} value={m}>{m}</option>)}
                  </select>
                </div>
              )}
            </>
          )}

          {(activePortal === 'soi' || activePortal === 'quarterly' || activePortal === 'ccc') ? (
            <div className={styles.inputGroup}>
              <label>Quarter</label>
              <select
                className={styles.selectField}
                value={quarter}
                onChange={(e) => setQuarter(e.target.value)}
                required
              >
                <option value="">Select Quarter</option>
                <option value="First">First</option>
                <option value="Second">Second</option>
                <option value="Third">Third</option>
                <option value="Fourth">Fourth</option>
              </select>
            </div>
          ) : activePortal === 'annual' ? (
            <div className={styles.inputGroup}>
              <label>Report Type</label>
              <select
                className={styles.selectField}
                value={reportType}
                onChange={(e) => setReportType(e.target.value)}
                required
              >
                <option value="">Select Report Type</option>
                <option value="Annual Workplan">Annual Workplan</option>
                <option value="Strategic Plan">Strategic Plan</option>
                <option value="Annual Statement on Statuses">Annual Statement on Statuses</option>
                <option value="Annual Internal Audit Report">Annual Internal Audit Report</option>
              </select>
            </div>
          ) : (
            <div className={styles.inputGroup}>
              <label>Report Type</label>
              <input
                type="text"
                className={styles.inputField}
                value={period}
                onChange={(e) => setPeriod(e.target.value)}
                placeholder="e.g. Audit Report on Payroll"
                required
              />
            </div>
          )}

          <div className={styles.inputGroup}>
            <label>Sender Name</label>
            <input
              type="text"
              className={styles.inputField}
              value={senderName}
              onChange={(e) => setSenderName(e.target.value)}
              required
            />
          </div>

          <div className={styles.inputGroup}>
            <label>Sender Email</label>
            <input
              type="email"
              className={styles.inputField}
              value={senderEmail}
              onChange={(e) => setSenderEmail(e.target.value)}
              required
            />
          </div>

          <FileUpload files={files} setFiles={setFiles} />

          <button
            type="submit"
            className={styles.submitButton}
            disabled={submitting}
          >
            {submitting ? 'Submitting...' : 'Submit Report'}
          </button>
        </form>
      </div>
    </div>
  );
}

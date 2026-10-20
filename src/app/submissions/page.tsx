'use client';

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { onAuthStateChanged, User } from 'firebase/auth';
import { auth } from '@/lib/firebase';
import { getCachedConfigDoc, getCachedUserProfile } from '@/lib/firebase-cache';
import styles from './submissions.module.css';
import FileUpload from '@/components/submissions/FileUpload';
import { REGIONS, MMDA_DATA, RCC_DATA } from '@/data/mmda_data';
import { MDA_DATA, SOE_DATA } from '@/data/mda_data';

const SUBMISSION_DISABLED_MESSAGE = 'Please use your normal submissions channel until the Agency approves the use of this app';

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
  const [status] = useState<{ type: 'success' | 'error', message: string } | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

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

  const triggerDisabledToast = (e?: React.SyntheticEvent) => {
    if (e) {
      e.preventDefault();
      e.stopPropagation();
    }
    setToastMessage(SUBMISSION_DISABLED_MESSAGE);
  };

  useEffect(() => {
    if (!toastMessage) return;
    const timer = setTimeout(() => {
      setToastMessage(null);
    }, 4500);
    return () => clearTimeout(timer);
  }, [toastMessage]);

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
        const [userData, portalData, deadlinesData] = await Promise.all([
          getCachedUserProfile(user.uid),
          getCachedConfigDoc<{ welcomeNotice?: string }>('portal'),
          getCachedConfigDoc<{ json?: string }>('deadlines'),
        ]);

        if (userData) {
          setSenderName(`${userData.firstName || ''} ${userData.surname || ''}`.trim());
          setSenderEmail(String(userData.email || ''));

          // Pre-fill institution data from profile
          if (userData.institutionType) {
            const rawType = String(userData.institutionType);
            const type = (rawType === 'MDA' || rawType === 'SOE' || rawType === 'RCC' || rawType === 'MMDA')
              ? rawType
              : (rawType.includes('RCC') ? 'RCC' : 'MMDA');
            setInstitutionType(type);

            if (type === 'MDA' || type === 'SOE') {
              setInstitutionName(String(userData.institutionName || userData.institution || ''));
            } else if (type === 'RCC') {
              const reg = String(userData.region || '');
              setRegion(reg);
              setMmda(String(userData.institution || RCC_DATA[reg] || ''));
            } else {
              setRegion(String(userData.region || ''));
              const profInstitution = String(userData.institution || '');
              setMmda(profInstitution);
            }
          }
        }

        if (portalData?.welcomeNotice) {
          setPortalConfig({ description: portalData.welcomeNotice });
        }

        // Parse Deadlines (json from config/deadlines)
        try {
          let fetchedDeadlines: Deadline[] = [];
          if (deadlinesData?.json) {
            const parsed = JSON.parse(deadlinesData.json);
            fetchedDeadlines = Array.isArray(parsed) ? parsed : [];
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
    if (typeof window !== 'undefined') {
      const params = new URLSearchParams(window.location.search);
      const typeParam = params.get('type') as SubmissionType;
      if (typeParam && ['quarterly', 'annual', 'ccc', 'soi', 'other'].includes(typeParam)) {
        openPortal(typeParam);
      }
    }
  }, []);

  useEffect(() => {
    if (activePortal) {
      const draft = { institutionType, institutionName, region, mmda, period, quarter, reportType };
      localStorage.setItem(`draft_${activePortal}`, JSON.stringify(draft));
    }
  }, [institutionType, institutionName, region, mmda, period, quarter, reportType, activePortal]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    triggerDisabledToast();
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

        {toastMessage && (
          <div className={styles.toastContainer} role="alert" aria-live="polite">
            <div className={styles.toast}>
              <span className={styles.toastIcon}>⚠️</span>
              <span className={styles.toastText}>{toastMessage}</span>
              <button
                type="button"
                className={styles.toastClose}
                onClick={() => setToastMessage(null)}
                aria-label="Close notification"
              >
                ×
              </button>
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
              onChange={(e) => {
                const nextType = e.target.value;
                setInstitutionType(nextType);
                setInstitutionName('');
                setRegion('');
                setMmda('');
              }}
            >
              <option value="MDA">MDA</option>
              <option value="SOE">SOE</option>
              <option value="RCC">RCC</option>
              <option value="MMDA">MMDA</option>
            </select>
          </div>

          {(institutionType === 'MDA' || institutionType === 'SOE') ? (
            <div className={styles.inputGroup}>
              <label>Institution Name</label>
              <select
                className={styles.selectField}
                value={institutionName}
                onChange={(e) => setInstitutionName(e.target.value)}
                required
              >
                <option value="">Select Institution</option>
                {(institutionType === 'MDA' ? MDA_DATA : SOE_DATA).map(inst => (
                  <option key={inst} value={inst}>{inst}</option>
                ))}
              </select>
            </div>
          ) : institutionType === 'RCC' ? (
            <div className={styles.inputGroup}>
              <label>Region</label>
              <select
                className={styles.selectField}
                value={region}
                onChange={(e) => {
                  const reg = e.target.value;
                  setRegion(reg);
                  setMmda(RCC_DATA[reg] || '');
                }}
                required
              >
                <option value="">Select Region</option>
                {REGIONS.map(r => <option key={r} value={r}>{r}</option>)}
              </select>
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
                  <label>Institution (MMDA)</label>
                  <select
                    className={styles.selectField}
                    value={mmda}
                    onChange={(e) => setMmda(e.target.value)}
                    required
                  >
                    <option value="">Select MMDA</option>
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
                <option value="Annual IA Performance Report">Annual IA Performance Report</option>
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

          <div className={styles.submissionNotice}>
            <span className={styles.noticeIcon}>⚠️</span>
            <span>Please use your normal submissions channel until the Agency approves the use of this app</span>
          </div>

          <button
            type="button"
            className={`${styles.submitButton} ${styles.disabledButton}`}
            onClick={triggerDisabledToast}
            aria-disabled="true"
            title="Submissions temporarily disabled"
          >
            Submit Report
          </button>
        </form>
      </div>

      {toastMessage && (
        <div className={styles.toastContainer} role="alert" aria-live="polite">
          <div className={styles.toast}>
            <span className={styles.toastIcon}>⚠️</span>
            <span className={styles.toastText}>{toastMessage}</span>
            <button
              type="button"
              className={styles.toastClose}
              onClick={() => setToastMessage(null)}
              aria-label="Close notification"
            >
              ×
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

'use client';

import { useCallback, useEffect, useState } from 'react';
import { onAuthStateChanged } from 'firebase/auth';
import { collection, doc, getDocs, limit, onSnapshot, query, where } from 'firebase/firestore';
import { auth, db } from '@/lib/firebase';
import { getCachedConfigDoc, getCachedUserProfile, invalidateAllCaches } from '@/lib/firebase-cache';
import { useRouter } from 'next/navigation';
import Image from 'next/image';
import styles from './page.module.css';

interface NoticeConfig {
  title: string;
  content: string;
}

interface WelcomeConfig {
  title: string;
  description: string;
}

interface Seminar {
  id: string;
  topic: string;
  date: string; // YYYY-MM-DD or parseable date
}

interface Deadline {
  report: string;
  dateString: string; // YYYY-MM-DD or parseable date
}

function parseLocalDate(dateStr: string): Date | null {
  if (!dateStr) return null;
  const clean = dateStr.trim();
  const isoMatch = clean.match(/^(\d{4})-(\d{1,2})-(\d{1,2})/);
  if (isoMatch) {
    const year = parseInt(isoMatch[1], 10);
    const month = parseInt(isoMatch[2], 10) - 1;
    const day = parseInt(isoMatch[3], 10);
    return new Date(year, month, day, 0, 0, 0, 0);
  }
  const parsed = new Date(clean);
  if (isNaN(parsed.getTime())) return null;
  parsed.setHours(0, 0, 0, 0);
  return parsed;
}

function isDeadlineMetBySubmissions(deadlineReport: string, submissions: Record<string, unknown>[]): boolean {
  const normDeadline = deadlineReport.toLowerCase().replace(/[^a-z0-9]/g, '');
  const quarters = ['1st', '2nd', '3rd', '4th'];
  const dlQuarter = quarters.find(q => normDeadline.includes(q));

  return submissions.some(sub => {
    const normRepName = String(sub.reportName || '').toLowerCase().replace(/[^a-z0-9]/g, '');
    const normCategory = String(sub.category || '').toLowerCase().replace(/[^a-z0-9]/g, '');

    if (dlQuarter) {
      const matchesQuarter = normRepName.includes(dlQuarter) || normCategory.includes(dlQuarter);
      if (!matchesQuarter) return false;

      if (normDeadline.includes('ccc') || normDeadline.includes('commitment')) {
        return sub.category === 'ccc' || normRepName.includes('ccc');
      }
      if (normDeadline.includes('status') || normDeadline.includes('implementation') || normDeadline.includes('soi')) {
        return sub.category === 'soi' || normRepName.includes('soi') || normRepName.includes('status');
      }
      return (sub.category === 'quarterly' || normRepName.includes('quarterly') || normRepName.includes('internalaudit')) &&
             !normRepName.includes('soi') && !normRepName.includes('ccc') && !normRepName.includes('status');
    }
    return sub.category === 'annual' || normRepName.includes('annual') || normRepName.includes('recommendation');
  });
}

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
  { report: "2025 Annual Statement on Status of Audit Recommendations Implementations", dateString: "2026-06-30" },
];

const DEFAULT_SEMINARS: Seminar[] = [
  { id: "1", topic: "Leveraging the IIA Competency Framework to Build an Effective Internal Audit Function", date: "2026-03-26" },
  { id: "2", topic: "Data Analytics for Auditors", date: "2026-11-12" }
];

export default function Home() {
  const [profile, setProfile] = useState<Record<string, unknown> | null>(null);
  const [loading, setLoading] = useState(true);

  const [welcome, setWelcome] = useState<WelcomeConfig>({
    title: 'Welcome!',
    description: 'This app aims to ease the submission of the various reports expected from Internal Audit Functions to the IAA and to make resources readily available and accessible to Internal Auditors and Stakeholders.'
  });

  const [notice, setNotice] = useState<NoticeConfig>({
    title: 'Notices',
    content: 'The IAA has relocated its office premises to Tumu Avenue, Kanda Accra – Ghana with Digital Address: GV-002-6511'
  });

  const [counts, setCounts] = useState({
    seminars: 0,
    submissions: 0,
    submissionsLabel: 'Pending Submissions',
    submissionsSubtitle: 'Next 30 Days'
  });
  const router = useRouter();

  const loadData = useCallback(async (uid: string) => {
    try {
      const [userData, welcomeData, noticeData, seminarsData, deadlinesData] = await Promise.all([
        getCachedUserProfile(uid),
        getCachedConfigDoc<WelcomeConfig>('welcome_note'),
        getCachedConfigDoc<NoticeConfig>('notices'),
        getCachedConfigDoc<{ json?: string }>('seminars'),
        getCachedConfigDoc<{ json?: string }>('deadlines'),
      ]);

      if (userData) {
        setProfile(userData);
        const isComplete = userData.isProfileComplete || userData.profileComplete || (
          userData.firstName &&
          userData.surname &&
          userData.phone &&
          (userData.institution || userData.institutionName)
        );
        if (!isComplete) {
          router.push('/profile-completion');
        }
      } else {
        router.push('/profile-completion');
      }

      if (welcomeData) {
        setWelcome(welcomeData);
      }
      if (noticeData) {
        setNotice(noticeData);
      }

      let seminars: Seminar[] = [];
      if (seminarsData?.json) {
        try {
          seminars = JSON.parse(seminarsData.json) as Seminar[];
        } catch {
          seminars = [];
        }
      }
      if (seminars.length === 0) {
        seminars = DEFAULT_SEMINARS;
      }

      const today = new Date();
      today.setHours(0, 0, 0, 0);
      const thirtyDaysFromNow = new Date();
      thirtyDaysFromNow.setDate(today.getDate() + 30);
      thirtyDaysFromNow.setHours(23, 59, 59, 999);

      const seminarCount = seminars.filter(sem => {
        const semDate = parseLocalDate(sem.date);
        if (!semDate) return false;
        return semDate >= today && semDate <= thirtyDaysFromNow;
      }).length;

      let deadlines: Deadline[] = [];
      if (deadlinesData?.json) {
        try {
          deadlines = JSON.parse(deadlinesData.json) as Deadline[];
        } catch {
          deadlines = [];
        }
      }
      if (deadlines.length === 0) {
        deadlines = DEFAULT_DEADLINES;
      }

      const upcomingDeadlines = deadlines.filter(d => {
        const dDate = parseLocalDate(d.dateString);
        if (!dDate) return false;
        return dDate >= today && dDate <= thirtyDaysFromNow;
      });

      const institution = String(userData?.institution || userData?.institutionName || '').trim();
      const isAdminOrNoInst = !institution || userData?.role === 'admin';

      let institutionSubmissions: Record<string, unknown>[] = [];
      if (!isAdminOrNoInst) {
        try {
          const q = query(collection(db, 'submissions'), where('institution', '==', institution));
          const snap = await getDocs(q);
          institutionSubmissions = snap.docs.map(doc => doc.data() as Record<string, unknown>);
        } catch {
          try {
            const snap = await getDocs(query(collection(db, 'submissions'), limit(300)));
            const targetNorm = institution.toLowerCase().replace(/[^a-z0-9]/g, '');
            institutionSubmissions = snap.docs
              .map(doc => doc.data() as Record<string, unknown>)
              .filter(s => String(s.institution || '').toLowerCase().replace(/[^a-z0-9]/g, '') === targetNorm);
          } catch (e) {
            console.error("Error fetching institution submissions:", e);
          }
        }
      }

      let submissionsLabel = 'Upcoming Deadlines';
      let submissionCount = upcomingDeadlines.length;
      let submissionsSubtitle = 'Next 30 Days';

      if (!isAdminOrNoInst) {
        submissionsLabel = 'Pending Submissions';
        const pendingCount = upcomingDeadlines.filter(d => !isDeadlineMetBySubmissions(d.report, institutionSubmissions)).length;
        submissionCount = pendingCount;
        submissionsSubtitle = pendingCount === 0 ? 'All Caught Up' : 'Due Soon';
      }

      setCounts({
        seminars: seminarCount,
        submissions: submissionCount,
        submissionsLabel,
        submissionsSubtitle
      });
    } catch (error) {
      console.error("Error fetching dashboard data:", error);
    }
  }, [router]);

  useEffect(() => {
    let syncUnsub: (() => void) | null = null;
    const authUnsub = onAuthStateChanged(auth, async (currentUser) => {
      if (currentUser) {
        await loadData(currentUser.uid);
        // Subscribe to real-time sync state updates (exact parity with Android syncToken)
        syncUnsub = onSnapshot(doc(db, 'config', 'sync_state'), () => {
          invalidateAllCaches();
          loadData(currentUser.uid);
        });
      }
      setLoading(false);
    });

    return () => {
      authUnsub();
      if (syncUnsub) syncUnsub();
    };
  }, [loadData]);

  if (loading) return <div className={styles.loading}>Loading...</div>;

  return (
    <div className={styles.container}>
      {/* Header with Logo and Gradient */}
      <header className={styles.header}>
        <div className={styles.logoContainer}>
          <Image
            src="/iaa_logo.png"
            alt="IAA Logo"
            width={240}
            height={100}
            className={styles.logo}
            style={{ width: 'auto', height: 'auto' }}
            priority
          />
        </div>
      </header>

      <div className={styles.content}>
        {/* Welcome Section */}
        <section className={styles.welcomeSection}>
          <h2>{profile?.firstName ? `Welcome, IA ${profile.firstName}` : welcome.title}</h2>
          <p>{welcome.description}</p>
        </section>

        {/* Quick Stats Row */}
        <div className={styles.statsRow}>
          <div className={styles.statItem} onClick={() => router.push('/trainings')}>
            <div className={styles.statIcon}>
              <svg width="24" height="24" viewBox="0 0 24 24" fill="var(--primary)">
                <path d="M5 13.18v2.81c0 .73.4 1.41 1.05 1.76l5 2.63c.59.31 1.29.31 1.88 0l5-2.63c.65-.34 1.05-1.02 1.05-1.76v-2.81l-6.58 3.47c-.59.31-1.29.31-1.88 0L5 13.18zM12 3L1 8.82 12 14.65 21.49 9.63V15h2V8.82L12 3z"/>
              </svg>
            </div>
            <div className={styles.statValue}>{counts.seminars.toString().padStart(2, '0')}</div>
            <div className={styles.statLabel}>Upcoming Seminars</div>
            <div style={{ fontSize: '0.65rem', fontWeight: 600, marginTop: '2px', color: 'var(--text-secondary)' }}>
              Next 30 Days
            </div>
          </div>
          <div className={styles.statItem} onClick={() => router.push('/submissions#deadlines')}>
            <div className={styles.statIcon}>
              <svg width="24" height="24" viewBox="0 0 24 24" fill="var(--primary)">
                <path d="M12 22c1.1 0 2-.9 2-2h-4c0 1.1.89 2 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z"/>
              </svg>
            </div>
            <div className={styles.statValue}>{counts.submissions.toString().padStart(2, '0')}</div>
            <div className={styles.statLabel}>{counts.submissionsLabel}</div>
            <div
              style={{
                fontSize: '0.65rem',
                fontWeight: 600,
                marginTop: '2px',
                color: counts.submissionsSubtitle === 'All Caught Up' ? '#2e7d32' : counts.submissionsSubtitle === 'Due Soon' ? '#e65100' : 'var(--text-secondary)'
              }}
            >
              {counts.submissionsSubtitle}
            </div>
          </div>
        </div>

        {/* Quick Access Section */}
        <section className={styles.quickAccess}>
          <h3 className={styles.sectionTitle}>Quick Access</h3>
          <div className={styles.quickAccessGrid}>
            <div className={styles.quickCard} onClick={() => router.push('/submissions?type=quarterly')}>
              <svg className={styles.cardIcon} width="32" height="32" viewBox="0 0 24 24" fill="currentColor">
                <path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm4 18H6V4h7v5h5v11zM8 15.01l1.41 1.41L11 14.84V19h2v-4.16l1.59 1.59L16 15.01 12.01 11 8 15.01z"/>
              </svg>
              <span>Submit Quarterly Report</span>
            </div>
            <div className={styles.quickCard} onClick={() => router.push('/submissions?type=ccc')}>
              <svg className={styles.cardIcon} width="32" height="32" viewBox="0 0 24 24" fill="currentColor">
                <path d="M13 3c-4.97 0-9 4.03-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42C8.27 19.99 10.51 21 13 21c4.97 0 9-4.03 9-9s-4.03-9-9-9zm-1 5v5l4.28 2.54.72-1.21-3.5-2.08V8H12z"/>
              </svg>
              <span>Submit CCC Review Report</span>
            </div>
            <div className={styles.quickCard} onClick={() => router.push('/resources?expand=legislations')}>
              <svg className={styles.cardIcon} width="32" height="32" viewBox="0 0 24 24" fill="currentColor">
                <path d="M18 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zM6 4h5v8l-2.5-1.5L6 12V4z"/>
              </svg>
              <span>Legislation</span>
            </div>
            <div className={styles.quickCard} onClick={() => router.push('/resources?expand=templates')}>
              <svg className={styles.cardIcon} width="32" height="32" viewBox="0 0 24 24" fill="currentColor">
                <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z"/>
              </svg>
              <span>Reporting Templates</span>
            </div>
          </div>
        </section>

        {/* Notices Section */}
        <section className={styles.noticesSection}>
          <div className={styles.noticeCard}>
            <div className={styles.noticeHeader}>
              <svg width="20" height="20" viewBox="0 0 24 24" fill="var(--primary)">
                <path d="M20 2H4c-1.1 0-1.99.9-1.99 2L2 22l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-7 9h-2V5h2v6zm0 4h-2v-2h2v2z"/>
              </svg>
              <h3>{notice.title}</h3>
            </div>
            <p>{notice.content}</p>
          </div>
        </section>
      </div>
    </div>
  );
}
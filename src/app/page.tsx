'use client';

import { useEffect, useState } from 'react';
import { onAuthStateChanged, User } from 'firebase/auth';
import { doc, getDoc } from 'firebase/firestore';
import { auth, db } from '@/lib/firebase';
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
  date: string; // YYYY-MM-DD
}

interface Deadline {
  report: string;
  dateString: string; // YYYY-MM-DD
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
  const [user, setUser] = useState<User | null>(null);
  const [profile, setProfile] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  const [welcome, setWelcome] = useState<WelcomeConfig>({
    title: 'Welcome!',
    description: 'This app aims to ease the submission of the various reports expected from Internal Audit Functions to the IAA and to make resources readily available and accessible to Internal Auditors and Stakeholders.'
  });

  const [notice, setNotice] = useState<NoticeConfig>({
    title: 'Notices',
    content: 'The IAA has relocated its office premises to Tumu Avenue, Kanda Accra – Ghana with Digital Address: GV-002-6511'
  });

  const [counts, setCounts] = useState({ seminars: 0, submissions: 0 });
  const router = useRouter();

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (currentUser) => {
      if (currentUser) {
        setUser(currentUser);

        // 1. Fetch User Profile
        const userDoc = await getDoc(doc(db, 'users', currentUser.uid));
        if (userDoc.exists()) {
          const userData = userDoc.data();
          setProfile(userData);

          // Profile completion check
          const isComplete = userData.isProfileComplete || (
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

        // 2. Fetch Dashboard Configuration (Same as Android App)
        try {
          // Fetch Welcome Note
          const welcomeSnap = await getDoc(doc(db, 'config', 'welcome_note'));
          if (welcomeSnap.exists()) {
            setWelcome(welcomeSnap.data() as WelcomeConfig);
          }

          // Fetch Notices
          const noticeSnap = await getDoc(doc(db, 'config', 'notices'));
          if (noticeSnap.exists()) {
            setNotice(noticeSnap.data() as NoticeConfig);
          }

          // Fetch and Calculate Seminar Counts
          const seminarsSnap = await getDoc(doc(db, 'config', 'seminars'));
          let seminarCount = 0;
          let seminars: Seminar[] = [];

          if (seminarsSnap.exists()) {
            const json = seminarsSnap.data().json;
            if (json) {
              seminars = JSON.parse(json) as Seminar[];
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

          seminarCount = seminars.filter(sem => {
            const semDate = new Date(sem.date);
            if (isNaN(semDate.getTime())) return true;
            return semDate >= today && semDate <= thirtyDaysFromNow;
          }).length;

          // Fetch and Calculate Deadline Counts
          const deadlinesSnap = await getDoc(doc(db, 'config', 'deadlines'));
          let submissionCount = 0;
          let deadlines: Deadline[] = [];

          if (deadlinesSnap.exists()) {
            const json = deadlinesSnap.data().json;
            if (json) {
              deadlines = JSON.parse(json) as Deadline[];
            }
          }

          if (deadlines.length === 0) {
            deadlines = DEFAULT_DEADLINES;
          }

          submissionCount = deadlines.filter(d => {
            const dDate = new Date(d.dateString);
            if (isNaN(dDate.getTime())) return true;
            return dDate >= today && dDate <= thirtyDaysFromNow;
          }).length;

          setCounts({
            seminars: seminarCount,
            submissions: submissionCount
          });

        } catch (error) {
          console.error("Error fetching dashboard data:", error);
        }

      }
      setLoading(false);
    });

    return () => unsubscribe();
  }, [router]);

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
          </div>
          <div className={styles.statItem} onClick={() => router.push('/submissions#deadlines')}>
            <div className={styles.statIcon}>
              <svg width="24" height="24" viewBox="0 0 24 24" fill="var(--primary)">
                <path d="M12 22c1.1 0 2-.9 2-2h-4c0 1.1.89 2 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z"/>
              </svg>
            </div>
            <div className={styles.statValue}>{counts.submissions.toString().padStart(2, '0')}</div>
            <div className={styles.statLabel}>Pending Submissions</div>
          </div>
        </div>

        {/* Quick Access Section */}
        <section className={styles.quickAccess}>
          <h3 className={styles.sectionTitle}>Quick Access</h3>
          <div className={styles.quickAccessGrid}>
            <div className={styles.quickCard} onClick={() => router.push('/submissions')}>
              <svg className={styles.cardIcon} width="32" height="32" viewBox="0 0 24 24" fill="currentColor">
                <path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm4 18H6V4h7v5h5v11zM8 15.01l1.41 1.41L11 14.84V19h2v-4.16l1.59 1.59L16 15.01 12.01 11 8 15.01z"/>
              </svg>
              <span>Submit Quarterly Report</span>
            </div>
            <div className={styles.quickCard} onClick={() => router.push('/submissions')}>
              <svg className={styles.cardIcon} width="32" height="32" viewBox="0 0 24 24" fill="currentColor">
                <path d="M13 3c-4.97 0-9 4.03-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42C8.27 19.99 10.51 21 13 21c4.97 0 9-4.03 9-9s-4.03-9-9-9zm-1 5v5l4.28 2.54.72-1.21-3.5-2.08V8H12z"/>
              </svg>
              <span>Submit CCC Review Report</span>
            </div>
            <div className={styles.quickCard} onClick={() => router.push('/resources')}>
              <svg className={styles.cardIcon} width="32" height="32" viewBox="0 0 24 24" fill="currentColor">
                <path d="M5.22 8.57l3.53 3.53 4.24-4.24L9.46 4.33a1.5 1.5 0 0 0-2.12 0L5.22 6.45a1.5 1.5 0 0 0 0 2.12zM16.54 11l-4.24 4.24 3.53 3.53a1.5 1.5 0 0 0 2.12 0l2.12-2.12a1.5 1.5 0 0 0 0-2.12L16.54 11zM10.5 15l-1.41-1.41L2.12 20.5a1 1 0 0 0 0 1.41l.71.71a1 1 0 0 0 1.41 0L10.5 15z"/>
              </svg>
              <span>Legislation</span>
            </div>
            <div className={styles.quickCard} onClick={() => router.push('/resources')}>
              <svg className={styles.cardIcon} width="32" height="32" viewBox="0 0 24 24" fill="currentColor">
                <path d="M4 6H2v14c0 1.1.9 2 2 2h14v-2H4V6zm16-4H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-1 9H9V9h10v2zm-4 4H9v-2h6v2zm4-8H9V5h10v2z"/>
              </svg>
              <span>Reporting Templates</span>
            </div>
          </div>
        </section>

        {/* Notices Card */}
        <div className={styles.noticeCard}>
          <div className={styles.noticeHeader}>
            <svg width="24" height="24" viewBox="0 0 24 24" fill="var(--primary)">
              <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12zM7 9h10v2H7zm0-3h10v2H7zm0 6h7v2H7z"/>
            </svg>
            <h4>{notice.title}</h4>
          </div>
          <p>{notice.content}</p>
        </div>
      </div>
    </div>
  );
}

'use client';

import React, { useState, useEffect } from 'react';
import { collection, doc, getDoc, onSnapshot, query, orderBy, where } from 'firebase/firestore';
import { db } from '@/lib/firebase';
import styles from './trainings.module.css';

// ─── Data Types (matching Android's @Keep data classes) ────────────────────
interface Seminar {
  id: string;
  topic: string;
  date: string;
  time: string;
  location: string;
  fee: string;
  registrationUrl: string;
}

interface DbDocument {
  id: string;
  name: string;
  category: string;
  downloadUrl: string;
  timestamp: number;
}

// ─── Default Seminars (same as Android's mockSeminars) ─────────────────────
const DEFAULT_SEMINARS: Seminar[] = [
  {
    id: '1',
    topic: 'Leveraging the IIA Competency Framework to Build an Effective Internal Audit Function',
    date: '2026-03-26',
    time: '02:00 PM - 04:00 PM',
    location: 'Online via Zoom',
    fee: 'Free',
    registrationUrl: 'https://us06web.zoom.us/meeting/register/CH2P5vejT7Gkf5VyGwwIbA',
  },
  {
    id: '2',
    topic: 'Data Analytics for Auditors',
    date: '2026-11-12',
    time: '01:00 PM - 05:00 PM',
    location: 'Online (Zoom)',
    fee: 'GH₵ 1,200.00',
    registrationUrl: 'https://forms.gle/example2',
  },
];

// ─── Static Video Tutorials (same as Android's TrainingsScreen) ────────────
const VIDEO_TUTORIALS = [
  { title: 'IAA Audit Methodology 101', url: 'https://www.youtube.com/results?search_query=internal+audit+methodology' },
  { title: 'Risk Assessment Techniques', url: 'https://www.youtube.com/results?search_query=audit+risk+assessment' },
  { title: 'Reporting Best Practices', url: 'https://www.youtube.com/results?search_query=internal+audit+reporting' },
];

// ─── Accordion Card ─────────────────────────────────────────────────────────
function AccordionCard({
  icon,
  title,
  subtitle,
  children,
}: {
  icon: React.ReactNode;
  title: string;
  subtitle: string;
  children: React.ReactNode;
}) {
  const [expanded, setExpanded] = useState(false);

  return (
    <div className={styles.card}>
      <button
        className={styles.cardHeader}
        onClick={() => setExpanded(!expanded)}
        aria-expanded={expanded}
      >
        <div className={styles.iconBox}>{icon}</div>
        <div className={styles.cardMeta}>
          <span className={styles.cardTitle}>{title}</span>
          <span className={styles.cardSubtitle}>{subtitle}</span>
        </div>
        <span
          className={styles.chevron}
          style={{ transform: expanded ? 'rotate(180deg)' : 'none' }}
        >
          ▼
        </span>
      </button>
      {expanded && <div className={styles.cardBody}>{children}</div>}
    </div>
  );
}

// ─── Format document name (same logic as resources page) ───────────────────
function formatName(name: string): string {
  let clean = name.replace(/\.(pdf|docx?|xlsx?|pptx?|txt|zip)$/i, '');
  clean = clean.replace(/[_-]/g, ' ');
  const acronyms = ['PFM', 'IAA', 'LGA', 'PPA', 'CCC', 'NACAP', 'LI', 'ACT'];
  return clean
    .split(' ')
    .map((word) => {
      const upper = word.toUpperCase();
      if (acronyms.includes(upper)) return upper;
      const small = ['and', 'of', 'for', 'the', 'in', 'on', 'with'];
      if (small.includes(word.toLowerCase())) return word.toLowerCase();
      return word.charAt(0).toUpperCase() + word.slice(1).toLowerCase();
    })
    .join(' ');
}

// ─── Main Page ──────────────────────────────────────────────────────────────
export default function TrainingsPage() {
  const [seminars, setSeminars] = useState<Seminar[]>([]);
  const [trainingDocs, setTrainingDocs] = useState<DbDocument[]>([]);
  const [loadingSeminars, setLoadingSeminars] = useState(true);
  const [loadingDocs, setLoadingDocs] = useState(true);

  // Fetch seminars from Firestore (same path as Android: config/seminars)
  useEffect(() => {
    async function fetchSeminars() {
      try {
        const snap = await getDoc(doc(db, 'config', 'seminars'));
        if (snap.exists()) {
          const json = snap.data().json;
          if (json) {
            const parsed: Seminar[] = JSON.parse(json);
            setSeminars(parsed.length > 0 ? parsed : DEFAULT_SEMINARS);
          } else {
            setSeminars(DEFAULT_SEMINARS);
          }
        } else {
          setSeminars(DEFAULT_SEMINARS);
        }
      } catch {
        setSeminars(DEFAULT_SEMINARS);
      } finally {
        setLoadingSeminars(false);
      }
    }
    fetchSeminars();
  }, []);

  // Listen to training_resources documents (same collection/filter as Android)
  useEffect(() => {
    const q = query(
      collection(db, 'portal_documents'),
      where('category', '==', 'training_resources'),
      orderBy('timestamp', 'desc')
    );
    const unsub = onSnapshot(
      q,
      (snap) => {
        setTrainingDocs(
          snap.docs.map((d) => ({ id: d.id, ...d.data() } as DbDocument))
        );
        setLoadingDocs(false);
      },
      () => setLoadingDocs(false)
    );
    return () => unsub();
  }, []);

  // SVG Icons
  const PlayIcon = (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
      <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 14.5v-9l6 4.5-6 4.5z" />
    </svg>
  );

  const SeminarIcon = (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
      <path d="M17 12h-5v5h5v-5zM16 1v2H8V1H6v2H5c-1.11 0-1.99.9-1.99 2L3 19c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2h-1V1h-2zm3 18H5V8h14v11z" />
    </svg>
  );

  const ResourcesIcon = (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
      <path d="M16.5 6V4H15V2h-2v2H5c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V6h-4.5zM9 18H7v-2h2v2zm0-4H7v-2h2v2zm0-4H7V8h2v2zm10 8h-8v-2h8v2zm0-4h-8v-2h8v2zm0-4h-8V8h8v2z" />
    </svg>
  );

  return (
    <div className={styles.page}>
      {/* ── Header ── */}
      <header className={styles.header}>
        <h1>Training Portal</h1>
        <p>
          Keep up with professional training modules and seminars available for
          skills development.
        </p>
      </header>

      <div className={styles.list}>
        {/* ── 1. Video Tutorials ── */}
        <AccordionCard
          icon={PlayIcon}
          title="Video Tutorials"
          subtitle="Step-by-step guides on relevant audit activities."
        >
          <div className={styles.divider} />
          {VIDEO_TUTORIALS.map((v) => (
            <a
              key={v.title}
              href={v.url}
              target="_blank"
              rel="noopener noreferrer"
              className={styles.videoItem}
            >
              <span className={styles.playDot}>▶</span>
              <span>{v.title}</span>
              <span className={styles.externalArrow}>↗️</span>
            </a>
          ))}
        </AccordionCard>

        {/* ── 2. Upcoming Seminars ── */}
        <AccordionCard
          icon={SeminarIcon}
          title="Upcoming Seminars"
          subtitle="Register for upcoming professional seminars."
        >
          <div className={styles.divider} />
          {loadingSeminars ? (
            <p className={styles.emptyText}>Loading seminars…</p>
          ) : seminars.length === 0 ? (
            <p className={styles.emptyText}>
              No upcoming seminars scheduled at the moment.
            </p>
          ) : (
            <div className={styles.seminarList}>
              {seminars.map((sem, idx) => (
                <div key={sem.id}>
                  {idx > 0 && <div className={styles.seminarDivider} />}
                  <SeminarCard seminar={sem} />
                </div>
              ))}
            </div>
          )}
        </AccordionCard>

        {/* ── 3. Training Resources ── */}
        <AccordionCard
          icon={ResourcesIcon}
          title="Training Resources"
          subtitle="Presentation files and resources."
        >
          <div className={styles.divider} />
          {loadingDocs ? (
            <p className={styles.emptyText}>Loading resources…</p>
          ) : trainingDocs.length === 0 ? (
            <p className={styles.emptyText}>No training resources available.</p>
          ) : (
            <div className={styles.docList}>
              {trainingDocs.map((doc) => (
                <a
                  key={doc.id}
                  href={doc.downloadUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className={styles.docItem}
                >
                  <span className={styles.docIcon}>📄</span>
                  <span className={styles.docName}>{formatName(doc.name)}</span>
                  <span className={styles.openIcon}>
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z" />
                    </svg>
                  </span>
                </a>
              ))}
            </div>
          )}
        </AccordionCard>
      </div>

      <div style={{ height: 100 }} />
    </div>
  );
}

// ─── Seminar Card (mirrors SeminarDetailItem in Android) ───────────────────
function SeminarCard({ seminar }: { seminar: Seminar }) {
  const formatDate = (dateStr: string) => {
    try {
      return new Date(dateStr).toLocaleDateString(undefined, {
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric',
      });
    } catch {
      return dateStr;
    }
  };

  return (
    <div className={styles.seminarCard}>
      <h3 className={styles.seminarTopic}>{seminar.topic}</h3>

      <div className={styles.seminarMeta}>
        <div className={styles.metaRow}>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" className={styles.metaIcon}>
            <path d="M17 12h-5v5h5v-5zM16 1v2H8V1H6v2H5c-1.11 0-1.99.9-1.99 2L3 19c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2h-1V1h-2zm3 18H5V8h14v11z" />
          </svg>
          <span>{formatDate(seminar.date)}</span>
        </div>

        {seminar.time && (
          <div className={styles.metaRow}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" className={styles.metaIcon}>
              <path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67V7z" />
            </svg>
            <span>{seminar.time}</span>
          </div>
        )}

        {seminar.location && (
          <div className={styles.metaRow}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" className={styles.metaIcon}>
              <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z" />
            </svg>
            <span>{seminar.location}</span>
          </div>
        )}

        {seminar.fee && (
          <div className={styles.metaRow}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" className={styles.metaIcon}>
              <path d="M11.8 10.9c-2.27-.59-3-1.2-3-2.15 0-1.09 1.01-1.85 2.7-1.85 1.78 0 2.44.85 2.5 2.1h2.21c-.07-1.72-1.12-3.3-3.21-3.81V3h-3v2.16c-1.94.42-3.5 1.68-3.5 3.61 0 2.31 1.91 3.46 4.7 4.13 2.5.6 3 1.48 3 2.41 0 .69-.49 1.79-2.7 1.79-2.06 0-2.87-.92-2.98-2.1h-2.2c.12 2.19 1.76 3.42 3.68 3.83V21h3v-2.15c1.95-.37 3.5-1.5 3.5-3.55 0-2.84-2.43-3.81-4.7-4.4z" />
            </svg>
            <span className={styles.feeText}>Fee: {seminar.fee}</span>
          </div>
        )}
      </div>

      {seminar.registrationUrl && (
        <a
          href={seminar.registrationUrl}
          target="_blank"
          rel="noopener noreferrer"
          className={styles.registerBtn}
        >
          Register Now
        </a>
      )}
    </div>
  );
}

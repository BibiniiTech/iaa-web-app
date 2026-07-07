'use client';

import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import { onAuthStateChanged } from 'firebase/auth';
import {
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  orderBy,
  query,
  setDoc,
} from 'firebase/firestore';
import { deleteObject, getDownloadURL, ref, uploadBytes } from 'firebase/storage';
import { auth, db, storage } from '@/lib/firebase';
import { MMDA_DATA, REGIONS } from '@/data/mmda_data';
import styles from './admin.module.css';

type Tab = 'home' | 'emails' | 'pfmDocs' | 'dates' | 'voting' | 'submissions';
type CategoryId = 'quarterly' | 'annual' | 'ccc' | 'soi' | 'other';
type PortalDocCategory = 'legislations' | 'guidelines' | 'templates' | 'checklists' | 'training_resources' | 'others';

type AdminUser = {
  firstName?: string;
  surname?: string;
};

type WelcomeConfig = {
  title: string;
  description: string;
};

type NoticeConfig = {
  title: string;
  content: string;
};

type ContactConfig = {
  address: string;
  phone: string;
  emails: string;
  specificDesks: string;
  locationQuery: string;
  facebookUrl: string;
  instagramUrl: string;
  xUrl: string;
};

type CloudConfig = {
  mda_to: string[];
  mda_cc: string[];
  mmda_to: string[];
  region_cc: Record<string, string[]>;
  mmda_institution_cc?: Record<string, Record<string, string[]>>;
};

type PFMSection = {
  title: string;
  status: string;
  institutions: string[];
};

type DbDocument = {
  id: string;
  name: string;
  category: PortalDocCategory | string;
  downloadUrl: string;
  timestamp: number;
};

type Deadline = {
  report: string;
  dateString: string;
};

type Seminar = {
  id: string;
  topic: string;
  date: string;
  time: string;
  location: string;
  fee: string;
  registrationUrl: string;
};

type VotingOption = {
  id: string;
  text: string;
  imageUrl: string;
};

type VotingCategory = {
  id: string;
  name: string;
  options: VotingOption[];
  allowMultiple: boolean;
};

type VotingConfig = {
  visible: boolean;
  header: string;
  categories: VotingCategory[];
};

type Submission = {
  id: string;
  userId?: string;
  category: string;
  type?: string;
  institutionType: string;
  institution: string;
  region: string;
  senderName: string;
  senderEmail: string;
  timestamp: number;
  fileNames: string[];
  filePaths?: string[];
  reportName: string;
  files?: { name: string; url: string }[];
};

const TABS: { id: Tab; label: string }[] = [
  { id: 'home', label: 'Home & Contact' },
  { id: 'emails', label: 'Submission Emails' },
  { id: 'pfmDocs', label: 'PFM & Portal Docs' },
  { id: 'dates', label: 'Dates & Seminars' },
  { id: 'voting', label: 'Voting' },
  { id: 'submissions', label: 'Submissions Monitor' },
];

const CATEGORY_OPTIONS: { id: CategoryId; label: string }[] = [
  { id: 'quarterly', label: 'Quarterly Report' },
  { id: 'annual', label: 'Annual Report' },
  { id: 'ccc', label: 'CCC Review Report' },
  { id: 'soi', label: 'Status of Implementation (SOI)' },
  { id: 'other', label: 'Other Reports' },
];

const PORTAL_DOC_CATEGORIES: PortalDocCategory[] = [
  'legislations',
  'guidelines',
  'templates',
  'checklists',
  'training_resources',
  'others',
];

const PFM_STATUSES = ['HIGHLY COMPLIANT', 'COMPLIANT', 'MODERATELY COMPLIANT', 'LEAST COMPLIANT'];

const DEFAULT_WELCOME: WelcomeConfig = {
  title: 'Welcome!',
  description:
    'This app aims to ease the submission of the various reports expected from Internal Audit Functions to the IAA and to make resources readily available and accessible to Internal Auditors and Stakeholders.',
};

const DEFAULT_NOTICE: NoticeConfig = {
  title: 'Notices',
  content: 'The IAA has relocated its office premises to Tumu Avenue, Kanda Accra - Ghana with Digital Address: GV-002-6511',
};

const DEFAULT_CONTACT: ContactConfig = {
  address: 'Tumu Avenue, Kanda Accra - Ghana\nDigital Address: GV-002-6511',
  phone: '+233 (0) 362 196 941',
  emails: 'info@iaa.gov.gh\niaamails@iaa.gov.gh',
  specificDesks: 'CCCC: +233 (0) 362 196 941\nNACAP: +233 (0) 362 196 941\nTrainings: +233 (0) 362 196 941',
  locationQuery: 'Internal+Audit+Agency+Tumu+Avenue+Kanda+Accra',
  facebookUrl: 'https://web.facebook.com/internalauditagency/?_rdc=1&_rdr#',
  instagramUrl: 'https://www.instagram.com/iaa.gov.gh/',
  xUrl: 'https://x.com/IAA_Ghana',
};

const DEFAULT_DEADLINES: Deadline[] = [
  { report: '1st Quarter Internal Audit Report', dateString: '2026-04-30' },
  { report: '2nd Quarter Internal Audit Report', dateString: '2026-07-31' },
  { report: '3rd Quarter Internal Audit Report', dateString: '2026-10-31' },
  { report: 'Status of Implementation on 4th Quarter IA Report', dateString: '2026-04-30' },
  { report: 'Status of Implementation on 1st Quarter IA Report', dateString: '2026-07-31' },
  { report: 'Status of Implementation on 2nd Quarter IA Report', dateString: '2026-10-31' },
  { report: '1st Quarter Commitment Control Compliance (CCC) Review Report', dateString: '2026-04-10' },
  { report: '2nd Quarter Commitment Control Compliance (CCC) Review Report', dateString: '2026-07-10' },
  { report: '3rd Quarter Commitment Control Compliance (CCC) Review Report', dateString: '2026-10-10' },
  { report: '2025 Annual Statement on Status of Audit Recommendations Implementations', dateString: '2026-06-30' },
];

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
    fee: 'GHS 1,200.00',
    registrationUrl: 'https://forms.gle/example2',
  },
];

const REPORT_FILTERS = [
  'All Reports',
  'First Quarter Report',
  'Second Quarter Report',
  'Third Quarter Report',
  'Fourth Quarter Report',
  'Annual Workplan',
  'Strategic Plan',
  'Annual Statement on Statuses',
  'Annual Internal Audit Report',
  'CCC Review Report',
  'First Quarter SOI Report',
  'Second Quarter SOI Report',
  'Third Quarter SOI Report',
  'Fourth Quarter SOI Report',
  'Other Reports',
];

function emptyRegionCc(): Record<string, string[]> {
  return Object.fromEntries(REGIONS.map((region) => [region, []]));
}

function emptyInstitutionCc(): Record<string, Record<string, string[]>> {
  return Object.fromEntries(
    REGIONS.map((region) => [
      region,
      Object.fromEntries((MMDA_DATA[region] || []).map((institution) => [institution, []])),
    ])
  );
}

function defaultCloudConfig(category: CategoryId): CloudConfig {
  return {
    mda_to: ['clemzy93@gmail.com'],
    mda_cc: [],
    mmda_to: ['clemzy93@gmail.com'],
    region_cc: emptyRegionCc(),
    mmda_institution_cc: emptyInstitutionCc(),
  };
}

function defaultRecipientMap(): Record<CategoryId, CloudConfig> {
  return {
    quarterly: defaultCloudConfig('quarterly'),
    annual: defaultCloudConfig('annual'),
    ccc: defaultCloudConfig('ccc'),
    soi: defaultCloudConfig('soi'),
    other: defaultCloudConfig('other'),
  };
}

function splitEmails(value: string): string[] {
  return value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}

function asStringList(value: unknown): string[] {
  return Array.isArray(value) ? value.filter((item): item is string => typeof item === 'string').map((item) => item.trim()).filter(Boolean) : [];
}

function parseRecipientEmails(json: string): Record<CategoryId, CloudConfig> {
  const defaults = defaultRecipientMap();
  if (!json.trim()) return defaults;

  try {
    const parsed = JSON.parse(json) as Record<string, unknown>;
    const hasCategoryKeys = CATEGORY_OPTIONS.some(({ id }) => id in parsed);

    if (hasCategoryKeys) {
      const next = { ...defaults };
      CATEGORY_OPTIONS.forEach(({ id }) => {
        const raw = parsed[id] as Partial<CloudConfig> | undefined;
        if (!raw) return;
        next[id] = normalizeCloudConfig(raw, defaults[id]);
      });
      return next;
    }

    const legacy = normalizeCloudConfig(parsed as Partial<CloudConfig>, defaults.quarterly);
    return { ...defaults, quarterly: legacy };
  } catch {
    return defaults;
  }
}

function normalizeCloudConfig(raw: Partial<CloudConfig>, fallback: CloudConfig): CloudConfig {
  return {
    mda_to: asStringList(raw.mda_to).length ? asStringList(raw.mda_to) : fallback.mda_to,
    mda_cc: asStringList(raw.mda_cc).length ? asStringList(raw.mda_cc) : fallback.mda_cc,
    mmda_to: asStringList(raw.mmda_to).length ? asStringList(raw.mmda_to) : fallback.mmda_to,
    region_cc: { ...fallback.region_cc, ...(raw.region_cc || {}) },
    mmda_institution_cc: raw.mmda_institution_cc || fallback.mmda_institution_cc,
  };
}

function parseJsonList<T>(json: string, fallback: T[]): T[] {
  try {
    const parsed = JSON.parse(json);
    return Array.isArray(parsed) ? parsed : fallback;
  } catch {
    return fallback;
  }
}

function parseTimestamp(timestamp: unknown): number {
  if (typeof timestamp === 'number') return timestamp;
  if (typeof timestamp === 'string') {
    const parsed = Date.parse(timestamp);
    return Number.isNaN(parsed) ? Date.now() : parsed;
  }
  if (timestamp && typeof timestamp === 'object' && 'toMillis' in timestamp && typeof timestamp.toMillis === 'function') {
    return timestamp.toMillis();
  }
  if (timestamp && typeof timestamp === 'object' && 'toDate' in timestamp && typeof timestamp.toDate === 'function') {
    return timestamp.toDate().getTime();
  }
  return Date.now();
}

function formatDateTime(timestamp: number): string {
  return new Date(timestamp).toLocaleString(undefined, {
    month: 'short',
    day: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function safeFileName(fileName: string): string {
  return fileName.replace(/[\\/]/g, '_');
}

function sectionTitleForStatus(status: string): string {
  return `${status} INSTITUTIONS`;
}

function parsePfmCsv(csv: string): { mda: PFMSection[]; mmda: PFMSection[] } | null {
  const lines = csv.split(/\r?\n/).map((line) => line.trim()).filter(Boolean);
  if (lines.length < 2) return null;

  const groups = {
    mda: Object.fromEntries(PFM_STATUSES.map((status) => [status, [] as string[]])),
    mmda: Object.fromEntries(PFM_STATUSES.map((status) => [status, [] as string[]])),
  };

  lines.slice(1).forEach((line) => {
    const [categoryRaw, nameRaw, statusRaw] = line.split(',').map((part) => part.trim());
    const status = statusRaw?.toUpperCase();
    if (!nameRaw || !PFM_STATUSES.includes(status)) return;
    if (categoryRaw?.toUpperCase() === 'MDA') groups.mda[status].push(nameRaw);
    if (categoryRaw?.toUpperCase() === 'MMDA') groups.mmda[status].push(nameRaw);
  });

  return {
    mda: PFM_STATUSES.map((status) => ({ title: sectionTitleForStatus(status), status, institutions: groups.mda[status] })),
    mmda: PFM_STATUSES.map((status) => ({ title: sectionTitleForStatus(status), status, institutions: groups.mmda[status] })),
  };
}

function getSubmissionClass(submission: Submission): string {
  const date = new Date(submission.timestamp);
  const subYear = date.getFullYear();
  const subMonth = date.getMonth(); // 0-based
  const subDay = date.getDate();
  const currentYear = new Date().getFullYear();

  const category = (submission.category || submission.type || '').toLowerCase();
  const reportName = (submission.reportName || '').toLowerCase();

  // 1. Quarterly IA Reports
  if (category === 'quarterly' || reportName.includes('quarter report')) {
    if (subYear === currentYear) {
      if (reportName.includes('fourth')) {
        return subMonth === 0 ? styles.onTimeRow : subMonth > 0 ? styles.lateRow : '';
      }
      if (reportName.includes('first')) {
        return subMonth === 3 ? styles.onTimeRow : subMonth > 3 ? styles.lateRow : '';
      }
      if (reportName.includes('second')) {
        return subMonth === 6 ? styles.onTimeRow : subMonth > 6 ? styles.lateRow : '';
      }
      if (reportName.includes('third')) {
        return subMonth === 9 ? styles.onTimeRow : subMonth > 9 ? styles.lateRow : '';
      }
    }
    return '';
  }

  // 2. CCC Review Reports
  if (category === 'ccc' || reportName.includes('ccc review')) {
    let quarter = '';
    if (reportName.includes('first') || reportName.includes('1st')) quarter = 'First';
    else if (reportName.includes('second') || reportName.includes('2nd')) quarter = 'Second';
    else if (reportName.includes('third') || reportName.includes('3rd')) quarter = 'Third';
    else if (reportName.includes('fourth') || reportName.includes('4th')) quarter = 'Fourth';
    else {
      // Legacy fallback based on submission timestamp
      if ([11, 0, 1].includes(subMonth)) quarter = 'Fourth';
      else if ([2, 3, 4].includes(subMonth)) quarter = 'First';
      else if ([5, 6, 7].includes(subMonth)) quarter = 'Second';
      else quarter = 'Third';
    }

    if (subYear === currentYear) {
      if (quarter === 'Fourth') {
        return subMonth === 0 && subDay < 10 ? styles.onTimeRow : styles.lateRow;
      }
      if (quarter === 'First') {
        return subMonth < 3 || (subMonth === 3 && subDay < 10) ? styles.onTimeRow : styles.lateRow;
      }
      if (quarter === 'Second') {
        return subMonth < 6 || (subMonth === 6 && subDay < 10) ? styles.onTimeRow : styles.lateRow;
      }
      if (quarter === 'Third') {
        return subMonth < 9 || (subMonth === 9 && subDay < 10) ? styles.onTimeRow : styles.lateRow;
      }
    }
    return '';
  }

  return '';
}

function matchesReportFilter(submission: Submission, selectedReportType: string): boolean {
  if (selectedReportType === 'All Reports') return true;
  if (selectedReportType === 'Other Reports') return submission.category === 'other';
  if (selectedReportType === 'CCC Review Report') {
    return submission.category === 'ccc' || submission.reportName.includes('CCC Review Report');
  }
  return submission.reportName === selectedReportType;
}

export default function AdminPage() {
  const router = useRouter();
  const [user, setUser] = useState<AdminUser | null>(null);
  const [activeTab, setActiveTab] = useState<Tab>('home');
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const showMessage = useCallback((type: 'success' | 'error', text: string) => {
    setMessage({ type, text });
  }, []);

  const triggerSync = useCallback(async () => {
    await setDoc(doc(db, 'config', 'sync_state'), { lastUpdated: Date.now() }, { merge: true });
  }, []);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (currentUser) => {
      if (!currentUser) {
        router.push('/login');
        setLoading(false);
        return;
      }

      const userDoc = await getDoc(doc(db, 'users', currentUser.uid));
      if (!userDoc.exists() || userDoc.data().role !== 'admin') {
        router.push('/');
        setLoading(false);
        return;
      }

      setUser(userDoc.data() as AdminUser);
      setLoading(false);
    });

    return () => unsubscribe();
  }, [router]);

  if (loading) return <div className={styles.container}>Loading Admin Panel...</div>;

  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div>
          <h1>Admin Dashboard</h1>
          <p>Welcome, {[user?.firstName, user?.surname].filter(Boolean).join(' ') || 'Admin'}</p>
        </div>
      </header>

      <nav className={styles.tabs}>
        {TABS.map((tab) => (
          <button
            key={tab.id}
            className={`${styles.tabButton} ${activeTab === tab.id ? styles.activeTab : ''}`}
            onClick={() => setActiveTab(tab.id)}
            type="button"
          >
            {tab.label}
          </button>
        ))}
      </nav>

      {message && (
        <div className={message.type === 'success' ? styles.successMessage : styles.errorMessage}>
          {message.text}
        </div>
      )}

      <div className={styles.section}>
        {activeTab === 'home' && <HomeContactTab showMessage={showMessage} triggerSync={triggerSync} />}
        {activeTab === 'emails' && <RecipientEmailsTab showMessage={showMessage} triggerSync={triggerSync} />}
        {activeTab === 'pfmDocs' && <PfmDocsTab showMessage={showMessage} triggerSync={triggerSync} />}
        {activeTab === 'dates' && <DatesSeminarsTab showMessage={showMessage} triggerSync={triggerSync} />}
        {activeTab === 'voting' && <VotingTab showMessage={showMessage} triggerSync={triggerSync} />}
        {activeTab === 'submissions' && <SubmissionsMonitorTab showMessage={showMessage} />}
      </div>
    </div>
  );
}

function CollapsibleCard({
  title,
  subtitle,
  children,
  initiallyExpanded = true,
}: {
  title: string;
  subtitle?: string;
  children: React.ReactNode;
  initiallyExpanded?: boolean;
}) {
  const [expanded, setExpanded] = useState(initiallyExpanded);

  return (
    <section className={styles.card}>
      <button className={styles.cardHeader} type="button" onClick={() => setExpanded((value) => !value)}>
        <span>
          <strong>{title}</strong>
          {subtitle && <small>{subtitle}</small>}
        </span>
        <span>{expanded ? '−' : '+'}</span>
      </button>
      {expanded && <div className={styles.cardBody}>{children}</div>}
    </section>
  );
}

function HomeContactTab({
  showMessage,
  triggerSync,
}: {
  showMessage: (type: 'success' | 'error', text: string) => void;
  triggerSync: () => Promise<void>;
}) {
  const [welcome, setWelcome] = useState<WelcomeConfig>(DEFAULT_WELCOME);
  const [notice, setNotice] = useState<NoticeConfig>(DEFAULT_NOTICE);
  const [contact, setContact] = useState<ContactConfig>(DEFAULT_CONTACT);
  const [showDisclaimer, setShowDisclaimer] = useState(true);
  const [showPfmLeague, setShowPfmLeague] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    async function load() {
      const [welcomeSnap, noticeSnap, contactSnap, disclaimerSnap, pfmSnap] = await Promise.all([
        getDoc(doc(db, 'config', 'welcome_note')),
        getDoc(doc(db, 'config', 'notices')),
        getDoc(doc(db, 'config', 'contact_info')),
        getDoc(doc(db, 'config', 'show_disclaimer')),
        getDoc(doc(db, 'config', 'show_pfm_league')),
      ]);

      if (welcomeSnap.exists()) setWelcome({ ...DEFAULT_WELCOME, ...(welcomeSnap.data() as Partial<WelcomeConfig>) });
      if (noticeSnap.exists()) setNotice({ ...DEFAULT_NOTICE, ...(noticeSnap.data() as Partial<NoticeConfig>) });
      if (contactSnap.exists()) setContact({ ...DEFAULT_CONTACT, ...(contactSnap.data() as Partial<ContactConfig>) });
      if (disclaimerSnap.exists()) setShowDisclaimer(disclaimerSnap.data().show ?? true);
      if (pfmSnap.exists()) setShowPfmLeague(pfmSnap.data().show ?? true);
    }

    load().catch(() => showMessage('error', 'Failed to load home and contact configuration.'));
  }, [showMessage]);

  async function save() {
    setSaving(true);
    try {
      await Promise.all([
        setDoc(doc(db, 'config', 'welcome_note'), welcome),
        setDoc(doc(db, 'config', 'notices'), notice),
        setDoc(doc(db, 'config', 'contact_info'), contact),
        setDoc(doc(db, 'config', 'show_disclaimer'), { show: showDisclaimer }),
        setDoc(doc(db, 'config', 'show_pfm_league'), { show: showPfmLeague }),
      ]);
      await triggerSync();
      showMessage('success', 'Configurations saved successfully!');
    } catch {
      showMessage('error', 'Error saving some configurations.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className={styles.stack}>
      <CollapsibleCard title="Welcome Note">
        <Input label="Title" value={welcome.title} onChange={(title) => setWelcome({ ...welcome, title })} />
        <Textarea label="Description" value={welcome.description} onChange={(description) => setWelcome({ ...welcome, description })} />
      </CollapsibleCard>

      <CollapsibleCard title="Notices">
        <Input label="Title" value={notice.title} onChange={(title) => setNotice({ ...notice, title })} />
        <Textarea label="Content" value={notice.content} onChange={(content) => setNotice({ ...notice, content })} />
      </CollapsibleCard>

      <CollapsibleCard title="Contact & Location Details">
        <Textarea label="Office Address" value={contact.address} onChange={(address) => setContact({ ...contact, address })} />
        <Input label="Phone Contacts" value={contact.phone} onChange={(phone) => setContact({ ...contact, phone })} />
        <Textarea label="Email Addresses" value={contact.emails} onChange={(emails) => setContact({ ...contact, emails })} />
        <Textarea label="Specific Desks contacts" value={contact.specificDesks} onChange={(specificDesks) => setContact({ ...contact, specificDesks })} />
        <Input label="Location Query" value={contact.locationQuery} onChange={(locationQuery) => setContact({ ...contact, locationQuery })} />
        <Input label="Facebook URL" value={contact.facebookUrl} onChange={(facebookUrl) => setContact({ ...contact, facebookUrl })} />
        <Input label="Instagram URL" value={contact.instagramUrl} onChange={(instagramUrl) => setContact({ ...contact, instagramUrl })} />
        <Input label="X URL" value={contact.xUrl} onChange={(xUrl) => setContact({ ...contact, xUrl })} />
      </CollapsibleCard>

      <CollapsibleCard title="Disclaimer Screen Settings" subtitle="Enable or disable visibility of app screens for all users.">
        <Toggle label="Show Disclaimer Screen" checked={showDisclaimer} onChange={setShowDisclaimer} />
        <Toggle label="Show PFM League Screen" checked={showPfmLeague} onChange={setShowPfmLeague} />
      </CollapsibleCard>

      <button className={styles.saveButton} type="button" onClick={save} disabled={saving}>
        {saving ? 'Saving...' : 'Save Home & Contact Config'}
      </button>
    </div>
  );
}

function RecipientEmailsTab({
  showMessage,
  triggerSync,
}: {
  showMessage: (type: 'success' | 'error', text: string) => void;
  triggerSync: () => Promise<void>;
}) {
  const [configs, setConfigs] = useState<Record<CategoryId, CloudConfig>>(defaultRecipientMap);
  const [selectedCategory, setSelectedCategory] = useState<CategoryId>('quarterly');
  const [selectedRegion, setSelectedRegion] = useState(REGIONS[0]);
  const [selectedMmda, setSelectedMmda] = useState(MMDA_DATA[REGIONS[0]]?.[0] || '');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    async function load() {
      const snap = await getDoc(doc(db, 'config', 'recipient_emails'));
      setConfigs(parseRecipientEmails(snap.exists() ? String(snap.data().json || '') : ''));
    }

    load().catch(() => showMessage('error', 'Failed to load email configuration.'));
  }, [showMessage]);

  const config = configs[selectedCategory];
  const institutionCc = config.mmda_institution_cc?.[selectedRegion]?.[selectedMmda] || [];

  function updateConfig(next: CloudConfig) {
    setConfigs((current) => ({ ...current, [selectedCategory]: next }));
  }

  function updateRegionCc(value: string) {
    updateConfig({
      ...config,
      region_cc: { ...config.region_cc, [selectedRegion]: splitEmails(value) },
    });
  }

  function updateInstitutionCc(value: string) {
    const currentRegionMap = config.mmda_institution_cc?.[selectedRegion] || {};
    updateConfig({
      ...config,
      mmda_institution_cc: {
        ...(config.mmda_institution_cc || {}),
        [selectedRegion]: { ...currentRegionMap, [selectedMmda]: splitEmails(value) },
      },
    });
  }

  async function save() {
    setSaving(true);
    try {
      await setDoc(doc(db, 'config', 'recipient_emails'), { json: JSON.stringify(configs) });
      await triggerSync();
      showMessage('success', 'Email configurations saved successfully!');
    } catch {
      showMessage('error', 'Error saving email configurations.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className={styles.stack}>
      <CollapsibleCard title="Submission Recipient Emails" subtitle="Configure recipient email addresses for each report category.">
        <Select
          label="Report Submission Category"
          value={selectedCategory}
          options={CATEGORY_OPTIONS.map((item) => ({ value: item.id, label: item.label }))}
          onChange={(value) => setSelectedCategory(value as CategoryId)}
        />
        <Input label="MDA/SOE Report Recipient Emails" value={config.mda_to.join(', ')} onChange={(value) => updateConfig({ ...config, mda_to: splitEmails(value) })} />
        <Input label="MDA/SOE Report CC Recipients" value={config.mda_cc.join(', ')} onChange={(value) => updateConfig({ ...config, mda_cc: splitEmails(value) })} />
        <Input label="MMDA Report Recipient Emails" value={config.mmda_to.join(', ')} onChange={(value) => updateConfig({ ...config, mmda_to: splitEmails(value) })} />

        <div className={styles.divider} />
        <h3 className={styles.inlineTitle}>Regional CC configuration</h3>
        <Select
          label="Select Ghana Region"
          value={selectedRegion}
          options={REGIONS.map((region) => ({ value: region, label: region }))}
          onChange={(value) => {
            setSelectedRegion(value);
            setSelectedMmda(MMDA_DATA[value]?.[0] || '');
          }}
        />
        <Input label={`CC Recipients for ${selectedRegion} Region`} value={(config.region_cc[selectedRegion] || []).join(', ')} onChange={updateRegionCc} />

        <div className={styles.divider} />
        <h3 className={styles.inlineTitle}>Institution-Specific CC</h3>
        <Select
          label="Select Institution (MMDA)"
          value={selectedMmda}
          options={(MMDA_DATA[selectedRegion] || []).map((institution) => ({ value: institution, label: institution }))}
          onChange={setSelectedMmda}
        />
        <Input label={`CC Recipients for ${selectedMmda}`} value={institutionCc.join(', ')} onChange={updateInstitutionCc} />
      </CollapsibleCard>

      <button className={styles.saveButton} type="button" onClick={save} disabled={saving}>
        {saving ? 'Saving...' : 'Save Email Configurations'}
      </button>
    </div>
  );
}

function PfmDocsTab({
  showMessage,
  triggerSync,
}: {
  showMessage: (type: 'success' | 'error', text: string) => void;
  triggerSync: () => Promise<void>;
}) {
  const [selectedCategory, setSelectedCategory] = useState<PortalDocCategory>('legislations');
  const [documents, setDocuments] = useState<DbDocument[]>([]);
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [processingCsv, setProcessingCsv] = useState(false);
  const [uploading, setUploading] = useState(false);

  const loadDocuments = useCallback(async () => {
    const snap = await getDocs(query(collection(db, 'portal_documents'), orderBy('timestamp', 'desc')));
    setDocuments(
      snap.docs
        .map((item) => ({ id: item.id, ...item.data() } as DbDocument))
        .filter((item) => item.category === selectedCategory)
    );
  }, [selectedCategory]);

  useEffect(() => {
    Promise.resolve()
      .then(loadDocuments)
      .catch(() => showMessage('error', 'Failed to load portal documents.'));
  }, [loadDocuments, showMessage]);

  async function handleCsv(file: File | undefined) {
    if (!file) return;
    setProcessingCsv(true);
    try {
      const parsed = parsePfmCsv(await file.text());
      if (!parsed) {
        showMessage('error', 'Failed to parse CSV. Verify format: Category,InstitutionName,Status');
        return;
      }

      await Promise.all([
        setDoc(doc(db, 'config', 'pfm_league_mda'), { json: JSON.stringify(parsed.mda) }),
        setDoc(doc(db, 'config', 'pfm_league_mmda'), { json: JSON.stringify(parsed.mmda) }),
      ]);
      await triggerSync();
      showMessage('success', 'PFM League table updated successfully!');
    } catch {
      showMessage('error', 'Failed to parse CSV. Verify format: Category,InstitutionName,Status');
    } finally {
      setProcessingCsv(false);
    }
  }

  async function uploadDocuments() {
    if (!selectedFiles.length) return;
    setUploading(true);
    try {
      const timestamp = Date.now();
      for (const file of selectedFiles) {
        const storageRef = ref(storage, `documents/${selectedCategory}/${timestamp}_${safeFileName(file.name)}`);
        await uploadBytes(storageRef, file);
        const downloadUrl = await getDownloadURL(storageRef);
        const docRef = doc(collection(db, 'portal_documents'));
        await setDoc(docRef, {
          id: docRef.id,
          name: file.name,
          category: selectedCategory,
          downloadUrl,
          timestamp,
        });
      }
      setSelectedFiles([]);
      await triggerSync();
      await loadDocuments();
      showMessage('success', 'Documents uploaded and logged successfully!');
    } catch {
      showMessage('error', 'Failed to upload documents.');
    } finally {
      setUploading(false);
    }
  }

  async function deleteDocument(documentItem: DbDocument) {
    try {
      await deleteDoc(doc(db, 'portal_documents', documentItem.id));
      if (documentItem.downloadUrl) {
        await deleteObject(ref(storage, documentItem.downloadUrl)).catch(() => undefined);
      }
      await triggerSync();
      await loadDocuments();
      showMessage('success', 'Document deleted.');
    } catch {
      showMessage('error', 'Failed to delete document.');
    }
  }

  return (
    <div className={styles.stack}>
      <CollapsibleCard
        title="PFM League Table Update"
        subtitle="Upload CSV with three columns: Category,InstitutionName,Status."
      >
        <input className={styles.input} type="file" accept=".csv,text/*" onChange={(event) => handleCsv(event.target.files?.[0])} />
        {processingCsv && <p className={styles.muted}>Processing CSV...</p>}
      </CollapsibleCard>

      <CollapsibleCard title="Portal Document Uploader">
        <Select
          label="Target Category"
          value={selectedCategory}
          options={PORTAL_DOC_CATEGORIES.map((category) => ({ value: category, label: category }))}
          onChange={(value) => {
            setSelectedCategory(value as PortalDocCategory);
            setSelectedFiles([]);
          }}
        />
        <input className={styles.input} type="file" multiple onChange={(event) => setSelectedFiles(Array.from(event.target.files || []))} />
        {selectedFiles.length > 0 && (
          <div className={styles.selectedList}>
            {selectedFiles.map((file) => (
              <span key={file.name}>{file.name}</span>
            ))}
          </div>
        )}
        <button className={styles.saveButton} type="button" onClick={uploadDocuments} disabled={uploading || selectedFiles.length === 0}>
          {uploading ? 'Uploading...' : `Confirm Upload ${selectedFiles.length} Files`}
        </button>

        <div className={styles.divider} />
        <h3 className={styles.inlineTitle}>Existing Documents in {selectedCategory}</h3>
        {documents.length === 0 ? (
          <p className={styles.muted}>No documents found in this category.</p>
        ) : (
          <div className={styles.list}>
            {documents.map((item) => (
              <div className={styles.listRow} key={item.id}>
                <a href={item.downloadUrl} target="_blank" rel="noopener noreferrer">{item.name}</a>
                <button className={styles.removeBtn} type="button" onClick={() => deleteDocument(item)}>Delete</button>
              </div>
            ))}
          </div>
        )}
      </CollapsibleCard>
    </div>
  );
}

function DatesSeminarsTab({
  showMessage,
  triggerSync,
}: {
  showMessage: (type: 'success' | 'error', text: string) => void;
  triggerSync: () => Promise<void>;
}) {
  const [deadlines, setDeadlines] = useState<Deadline[]>(DEFAULT_DEADLINES);
  const [seminars, setSeminars] = useState<Seminar[]>(DEFAULT_SEMINARS);
  const [newDeadline, setNewDeadline] = useState<Deadline>({ report: '', dateString: '' });
  const [newSeminar, setNewSeminar] = useState<Omit<Seminar, 'id'>>({ topic: '', date: '', time: '', location: '', fee: '', registrationUrl: '' });
  const [savingDeadlines, setSavingDeadlines] = useState(false);

  const load = useCallback(async () => {
    const [deadlinesSnap, seminarsSnap] = await Promise.all([
      getDoc(doc(db, 'config', 'deadlines')),
      getDoc(doc(db, 'config', 'seminars')),
    ]);
    setDeadlines(deadlinesSnap.exists() ? parseJsonList<Deadline>(String(deadlinesSnap.data().json || ''), DEFAULT_DEADLINES) : DEFAULT_DEADLINES);
    setSeminars(seminarsSnap.exists() ? parseJsonList<Seminar>(String(seminarsSnap.data().json || ''), DEFAULT_SEMINARS) : DEFAULT_SEMINARS);
  }, []);

  useEffect(() => {
    Promise.resolve()
      .then(load)
      .catch(() => showMessage('error', 'Failed to load dates and seminars.'));
  }, [load, showMessage]);

  async function saveDeadlines() {
    setSavingDeadlines(true);
    try {
      await setDoc(doc(db, 'config', 'deadlines'), { json: JSON.stringify(deadlines) });
      await triggerSync();
      showMessage('success', 'Deadlines saved!');
    } catch {
      showMessage('error', 'Error saving deadlines.');
    } finally {
      setSavingDeadlines(false);
    }
  }

  async function saveSeminars(nextSeminars: Seminar[]) {
    setSeminars(nextSeminars);
    await setDoc(doc(db, 'config', 'seminars'), { json: JSON.stringify(nextSeminars) });
    await triggerSync();
  }

  async function addSeminar() {
    if (!newSeminar.topic || !newSeminar.date || !newSeminar.time) return;
    try {
      await saveSeminars([...seminars, { ...newSeminar, id: Date.now().toString() }]);
      setNewSeminar({ topic: '', date: '', time: '', location: '', fee: '', registrationUrl: '' });
      showMessage('success', 'Seminar added!');
    } catch {
      showMessage('error', 'Error saving seminar.');
    }
  }

  async function deleteSeminar(id: string) {
    try {
      await saveSeminars(seminars.filter((seminar) => seminar.id !== id));
      showMessage('success', 'Seminar deleted.');
    } catch {
      showMessage('error', 'Error deleting seminar.');
    }
  }

  return (
    <div className={styles.stack}>
      <CollapsibleCard title="Submission Deadlines">
        <div className={styles.inlineForm}>
          <Input label="Report Name" value={newDeadline.report} onChange={(report) => setNewDeadline({ ...newDeadline, report })} />
          <Input label="YYYY-MM-DD" value={newDeadline.dateString} onChange={(dateString) => setNewDeadline({ ...newDeadline, dateString })} />
          <button
            className={styles.addBtn}
            type="button"
            onClick={() => {
              if (!newDeadline.report || !newDeadline.dateString) return;
              setDeadlines([...deadlines, newDeadline]);
              setNewDeadline({ report: '', dateString: '' });
            }}
          >
            Add Deadline
          </button>
        </div>
        <div className={styles.list}>
          {deadlines.map((deadline, index) => (
            <div className={styles.listRow} key={`${deadline.report}-${deadline.dateString}-${index}`}>
              <span><strong>{deadline.report}</strong><small>{deadline.dateString}</small></span>
              <button className={styles.removeBtn} type="button" onClick={() => setDeadlines(deadlines.filter((_, itemIndex) => itemIndex !== index))}>Delete</button>
            </div>
          ))}
        </div>
        <button className={styles.saveButton} type="button" onClick={saveDeadlines} disabled={savingDeadlines}>
          {savingDeadlines ? 'Saving...' : 'Confirm Deadlines Save'}
        </button>
      </CollapsibleCard>

      <CollapsibleCard title="Manage Seminars">
        <Input label="Topic Title" value={newSeminar.topic} onChange={(topic) => setNewSeminar({ ...newSeminar, topic })} />
        <div className={styles.twoCol}>
          <Input label="Date (e.g. Mar 26, 2026)" value={newSeminar.date} onChange={(date) => setNewSeminar({ ...newSeminar, date })} />
          <Input label="Time (e.g. 02:00 PM)" value={newSeminar.time} onChange={(time) => setNewSeminar({ ...newSeminar, time })} />
        </div>
        <div className={styles.twoCol}>
          <Input label="Location (Zoom/Accra)" value={newSeminar.location} onChange={(location) => setNewSeminar({ ...newSeminar, location })} />
          <Input label="Fee (GHS / Free)" value={newSeminar.fee} onChange={(fee) => setNewSeminar({ ...newSeminar, fee })} />
        </div>
        <Input label="Registration URL Link" value={newSeminar.registrationUrl} onChange={(registrationUrl) => setNewSeminar({ ...newSeminar, registrationUrl })} />
        <button className={styles.addBtn} type="button" onClick={addSeminar}>Add New Seminar</button>

        <div className={styles.divider} />
        <div className={styles.list}>
          {seminars.map((seminar) => (
            <div className={styles.listRow} key={seminar.id}>
              <span><strong>{seminar.topic}</strong><small>{seminar.date} | {seminar.time} | {seminar.location}</small></span>
              <button className={styles.removeBtn} type="button" onClick={() => deleteSeminar(seminar.id)}>Delete</button>
            </div>
          ))}
        </div>
      </CollapsibleCard>
    </div>
  );
}

function VotingTab({
  showMessage,
  triggerSync,
}: {
  showMessage: (type: 'success' | 'error', text: string) => void;
  triggerSync: () => Promise<void>;
}) {
  const [visible, setVisible] = useState(false);
  const [header, setHeader] = useState('');
  const [categories, setCategories] = useState<VotingCategory[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [exporting, setExporting] = useState(false);

  useEffect(() => {
    async function load() {
      const snap = await getDoc(doc(db, 'config', 'voting_config'));
      if (snap.exists()) {
        const data = snap.data();
        setVisible(data.visible ?? false);
        setHeader(data.header || '');
        setCategories(data.categories || []);
      }
      setLoading(false);
    }
    load().catch(() => showMessage('error', 'Failed to load voting configuration.'));
  }, [showMessage]);

  async function save() {
    setSaving(true);
    try {
      await setDoc(doc(db, 'config', 'voting_config'), {
        visible,
        header,
        categories,
      });
      await triggerSync();
      showMessage('success', 'Voting configuration saved!');
    } catch {
      showMessage('error', 'Error saving voting configuration.');
    } finally {
      setSaving(false);
    }
  }

  async function exportResults() {
    setExporting(true);
    try {
      const snap = await getDocs(collection(db, 'votes'));
      if (snap.empty) {
        showMessage('error', 'No votes recorded yet.');
        return;
      }

      const votes = snap.docs.map(d => d.data());

      let csv = 'Surname,First Name,Email,Contact,Institution Type,Region,Institution Name';
      categories.forEach(cat => {
        csv += `,"${cat.name}"`;
      });
      csv += '\n';

      votes.forEach(vote => {
        let row = `"${vote.surname || ''}","${vote.firstName || ''}","${vote.email || ''}","${vote.phone || ''}","${vote.institutionType || ''}","${vote.region || ''}","${vote.institution || ''}"`;
        categories.forEach(cat => {
          const selections = vote.selections?.[cat.id] || [];
          const optionTexts = selections.map((optId: string) => {
            return cat.options.find(o => o.id === optId)?.text || optId;
          });
          row += `,"${optionTexts.join('; ')}"`;
        });
        csv += row + '\n';
      });

      const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.setAttribute('href', url);
      link.setAttribute('download', `Voting_Results_${Date.now()}.csv`);
      link.style.visibility = 'hidden';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      showMessage('success', 'Results exported successfully!');
    } catch (err) {
      console.error(err);
      showMessage('error', 'Export failed.');
    } finally {
      setExporting(false);
    }
  }

  if (loading) return <p className={styles.muted}>Loading voting configuration...</p>;

  return (
    <div className={styles.stack}>
      <CollapsibleCard title="Voting Manager">
        <Toggle label="Voting Page Visibility" checked={visible} onChange={setVisible} />
        <div className={styles.divider} />
        <Input label="Main Voting Header (e.g. Favorite Foods)" value={header} onChange={setHeader} />

        {categories.map((cat, catIdx) => (
          <VotingCategoryEditor
            key={cat.id}
            category={cat}
            onChange={(updated) => {
              const next = [...categories];
              next[catIdx] = updated;
              setCategories(next);
            }}
            onDelete={() => {
              setCategories(categories.filter((_, i) => i !== catIdx));
            }}
          />
        ))}

        <button
          className={styles.addBtn}
          type="button"
          onClick={() => setCategories([...categories, {
            id: Date.now().toString(),
            name: '',
            options: [
              { id: '1', text: '', imageUrl: '' },
              { id: '2', text: '', imageUrl: '' }
            ],
            allowMultiple: false
          }])}
        >
          + Add New Category
        </button>

        <div className={styles.divider} />
        <button className={styles.saveButton} type="button" onClick={save} disabled={saving}>
          {saving ? 'Saving...' : 'Save Voting Configuration'}
        </button>

        <div className={styles.divider} />
        <button
          className={styles.saveButton}
          style={{ background: '#6c757d' }}
          type="button"
          onClick={exportResults}
          disabled={exporting}
        >
          {exporting ? 'Exporting...' : 'Export Voting Results to Excel (CSV)'}
        </button>
      </CollapsibleCard>
    </div>
  );
}

function VotingCategoryEditor({
  category,
  onChange,
  onDelete
}: {
  category: VotingCategory;
  onChange: (cat: VotingCategory) => void;
  onDelete: () => void;
}) {
  return (
    <div className={styles.card} style={{ margin: '10px 0', border: '1px solid #ddd' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
        <h4 style={{ margin: 0 }}>Category: {category.name || 'New Category'}</h4>
        <button className={styles.removeBtn} onClick={onDelete}>Delete Section</button>
      </div>

      <Input label="Category Heading" value={category.name} onChange={(name) => onChange({ ...category, name })} />

      <div style={{ marginTop: '15px' }}>
        {category.options.map((opt, optIdx) => (
          <VotingOptionEditor
            key={opt.id}
            option={opt}
            onChange={(updated) => {
              const next = [...category.options];
              next[optIdx] = updated;
              onChange({ ...category, options: next });
            }}
            onDelete={() => {
              onChange({ ...category, options: category.options.filter((_, i) => i !== optIdx) });
            }}
          />
        ))}
      </div>

      <button
        className={styles.addBtn}
        style={{ marginTop: '10px' }}
        type="button"
        onClick={() => onChange({
          ...category,
          options: [...category.options, { id: Date.now().toString(), text: '', imageUrl: '' }]
        })}
      >
        + Add New Option
      </button>

      <div className={styles.divider} />
      <Toggle label="Allow Multiple Selection" checked={category.allowMultiple} onChange={(val) => onChange({ ...category, allowMultiple: val })} />
    </div>
  );
}

function VotingOptionEditor({
  option,
  onChange,
  onDelete
}: {
  option: VotingOption;
  onChange: (opt: VotingOption) => void;
  onDelete: () => void;
}) {
  const [uploading, setUploading] = useState(false);

  async function handleImageUpload(file: File | undefined) {
    if (!file) return;
    setUploading(true);
    try {
      const storageRef = ref(storage, `voting/${Date.now()}_${file.name}`);
      await uploadBytes(storageRef, file);
      const url = await getDownloadURL(storageRef);
      onChange({ ...option, imageUrl: url });
    } catch (err) {
      console.error(err);
      alert('Image upload failed.');
    } finally {
      setUploading(false);
    }
  }

  return (
    <div style={{ display: 'flex', gap: '10px', alignItems: 'center', marginBottom: '10px' }}>
      <div style={{ width: '60px', height: '60px', background: '#eee', borderRadius: '4px', overflow: 'hidden', cursor: 'pointer', position: 'relative' }}>
        {option.imageUrl ? (
          <img src={option.imageUrl} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
        ) : (
          <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%', fontSize: '0.8rem' }}>No Img</div>
        )}
        <input
          type="file"
          accept="image/*"
          onChange={(e) => handleImageUpload(e.target.files?.[0])}
          style={{ position: 'absolute', top: 0, left: 0, width: '100%', height: '100%', opacity: 0, cursor: 'pointer' }}
        />
        {uploading && <div style={{ position: 'absolute', inset: 0, background: 'rgba(255,255,255,0.7)', display: 'flex', justifyContent: 'center', alignItems: 'center', fontSize: '0.6rem' }}>...</div>}
      </div>

      <div style={{ flex: 1 }}>
        <Input label="Option Text" value={option.text} onChange={(text) => onChange({ ...option, text })} />
      </div>

      <button className={styles.removeBtn} onClick={onDelete}>×</button>
    </div>
  );
}

function SubmissionsMonitorTab({
  showMessage,
}: {
  showMessage: (type: 'success' | 'error', text: string) => void;
}) {
  const [submissions, setSubmissions] = useState<Submission[]>([]);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const snap = await getDocs(query(collection(db, 'submissions'), orderBy('timestamp', 'desc')));
      setSubmissions(
        snap.docs.map((item) => {
          const data = item.data();
          return {
            id: item.id,
            category: String(data.category || data.type || ''),
            type: data.type ? String(data.type) : undefined,
            institutionType: String(data.institutionType || ''),
            institution: String(data.institution || ''),
            region: String(data.region || ''),
            senderName: String(data.senderName || ''),
            senderEmail: String(data.senderEmail || ''),
            timestamp: parseTimestamp(data.timestamp),
            fileNames: asStringList(data.fileNames),
            filePaths: asStringList(data.filePaths),
            reportName: String(data.reportName || ''),
            userId: data.userId ? String(data.userId) : undefined,
            files: Array.isArray(data.files) ? data.files as { name: string; url: string }[] : undefined,
          };
        })
      );
    } catch {
      showMessage('error', 'Failed to load submissions.');
    } finally {
      setLoading(false);
    }
  }, [showMessage]);

  useEffect(() => {
    Promise.resolve().then(load);
  }, [load]);

  if (loading) return <p className={styles.muted}>Loading submissions...</p>;

  return (
    <div className={styles.stack}>
      <div className={styles.sectionHeader}>
        <h2>Recent Submissions</h2>
        <button className={styles.addBtn} type="button" onClick={load}>Refresh</button>
      </div>
      <SubmissionsMonitorCard title="MDA Submissions" institutionType="MDA" submissions={submissions} />
      <SubmissionsMonitorCard title="RCC / MMDA Submissions" institutionType="RCC/MMDA" submissions={submissions} showRegionFilter />
    </div>
  );
}

function SubmissionsMonitorCard({
  title,
  institutionType,
  submissions,
  showRegionFilter = false,
}: {
  title: string;
  institutionType: string;
  submissions: Submission[];
  showRegionFilter?: boolean;
}) {
  const [selectedReportType, setSelectedReportType] = useState('All Reports');
  const [selectedRegion, setSelectedRegion] = useState('All Regions');

  const filtered = useMemo(() => {
    return submissions.filter((submission) => {
      return (
        submission.institutionType === institutionType &&
        matchesReportFilter(submission, selectedReportType) &&
        (!showRegionFilter || selectedRegion === 'All Regions' || submission.region === selectedRegion)
      );
    });
  }, [institutionType, selectedRegion, selectedReportType, showRegionFilter, submissions]);

  return (
    <CollapsibleCard title={title}>
      <div className={styles.twoCol}>
        <Select
          label="Filter Report"
          value={selectedReportType}
          options={REPORT_FILTERS.map((report) => ({ value: report, label: report }))}
          onChange={setSelectedReportType}
        />
        {showRegionFilter && (
          <Select
            label="Filter Region"
            value={selectedRegion}
            options={['All Regions', ...REGIONS].map((region) => ({ value: region, label: region }))}
            onChange={setSelectedRegion}
          />
        )}
      </div>

      {filtered.length === 0 ? (
        <p className={styles.muted}>No submissions found for the selected filters.</p>
      ) : (
        <div className={styles.submissionList}>
          {filtered.map((submission) => (
            <SubmissionItem key={submission.id} submission={submission} />
          ))}
        </div>
      )}
    </CollapsibleCard>
  );
}

function SubmissionItem({ submission }: { submission: Submission }) {
  const [expanded, setExpanded] = useState(false);
  const files = submission.files?.length ? submission.files.map((file) => file.name) : submission.fileNames;

  return (
    <div className={`${styles.submissionItem} ${getSubmissionClass(submission)}`}>
      <button type="button" className={styles.submissionSummary} onClick={() => setExpanded((value) => !value)}>
        <span>
          <strong>{submission.institution}</strong>
          <small>{submission.reportName || submission.category}</small>
        </span>
        <span>{expanded ? '−' : '+'}</span>
      </button>
      {expanded && (
        <div className={styles.submissionDetails}>
          <Detail label="Sender:" value={submission.senderName} />
          <Detail label="Email:" value={submission.senderEmail} />
          <Detail label="Date/Time:" value={formatDateTime(submission.timestamp)} />
          {submission.region && <Detail label="Region:" value={submission.region} />}
          <div className={styles.detailRow}>
            <strong>Files:</strong>
            <div className={styles.fileLinks}>
              {submission.files && submission.files.length > 0 ? (
                submission.files.map((file, idx) => (
                  <a key={idx} href={file.url} target="_blank" rel="noopener noreferrer" className={styles.fileLink}>
                    {file.name}
                  </a>
                ))
              ) : (submission.filePaths && submission.filePaths.length > 0) ? (
                submission.filePaths.map((path, idx) => (
                  <FileDownloadLink
                    key={idx}
                    path={path}
                    name={submission.fileNames[idx] || path.split('/').pop() || 'File'}
                  />
                ))
              ) : (submission.fileNames && submission.fileNames.length > 0 && submission.userId) ? (
                submission.fileNames.map((name, idx) => (
                  <FileDownloadLink
                    key={idx}
                    path={`submissions/${submission.userId}/${submission.category}/${name}`}
                    name={name}
                  />
                ))
              ) : (submission.fileNames && submission.fileNames.length > 0) ? (
                <span>{submission.fileNames.join(', ')}</span>
              ) : (
                <span className={styles.muted}>No files attached</span>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function FileDownloadLink({ path, name }: { path: string, name: string }) {
  const [url, setUrl] = useState<string | null>(null);
  const [error, setError] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleClick = async (e: React.MouseEvent) => {
    if (url) return; // Follow link
    e.preventDefault();
    setLoading(true);
    try {
      const downloadUrl = await getDownloadURL(ref(storage, path));
      setUrl(downloadUrl);
      window.open(downloadUrl, '_blank');
    } catch (err) {
      console.error("Error fetching download URL for path:", path, err);
      setError(true);
    } finally {
      setLoading(false);
    }
  };

  return (
    <a
      href={url || '#'}
      onClick={handleClick}
      target="_blank"
      rel="noopener noreferrer"
      className={`${styles.fileLink} ${error ? styles.errorLink : ''} ${loading ? styles.loadingLink : ''}`}
    >
      {name} {loading && '...'} {error && '(Error)'}
    </a>
  );
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <div className={styles.detailRow}>
      <strong>{label}</strong>
      <span>{value}</span>
    </div>
  );
}

function Input({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) {
  return (
    <label className={styles.formGroup}>
      <span>{label}</span>
      <input className={styles.input} value={value} onChange={(event) => onChange(event.target.value)} />
    </label>
  );
}

function Textarea({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) {
  return (
    <label className={styles.formGroup}>
      <span>{label}</span>
      <textarea className={styles.textarea} value={value} onChange={(event) => onChange(event.target.value)} />
    </label>
  );
}

function Select({
  label,
  value,
  options,
  onChange,
}: {
  label: string;
  value: string;
  options: { value: string; label: string }[];
  onChange: (value: string) => void;
}) {
  return (
    <label className={styles.formGroup}>
      <span>{label}</span>
      <select className={styles.input} value={value} onChange={(event) => onChange(event.target.value)}>
        {options.map((option) => (
          <option key={option.value} value={option.value}>{option.label}</option>
        ))}
      </select>
    </label>
  );
}

function Toggle({ label, checked, onChange }: { label: string; checked: boolean; onChange: (checked: boolean) => void }) {
  return (
    <label className={styles.toggleRow}>
      <span>{label}</span>
      <input type="checkbox" checked={checked} onChange={(event) => onChange(event.target.checked)} />
    </label>
  );
}

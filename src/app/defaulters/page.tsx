'use client';

import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { collection, getDocs, orderBy, query } from 'firebase/firestore';
import { db } from '@/lib/firebase';
import { getCachedConfigDoc } from '@/lib/firebase-cache';
import { MDA_DATA, SOE_DATA } from '@/data/mda_data';
import { MMDA_DATA, REGIONS } from '@/data/mmda_data';
import styles from './defaulters.module.css';

export interface MasterInstitution {
  name: string;
  type: 'MDA' | 'SOE' | 'RCC/MMDA';
  region?: string;
}

export interface SubmissionRecord {
  id: string;
  institution: string;
  institutionType?: string;
  category?: string;
  reportName?: string;
  period?: string;
  timestamp: number;
  region?: string;
  senderName?: string;
  senderEmail?: string;
}

export interface ReportDefinition {
  id: string;
  title: string;
  category: 'quarterly' | 'soi' | 'ccc' | 'annual';
  defaultDeadline: string; // YYYY-MM-DD
  reportNamePattern: string;
  quarterIdentifier?: string;
}

export interface CategoryGroup {
  id: string;
  title: string;
  icon: string;
  reports: ReportDefinition[];
}

const CATEGORY_GROUPS: CategoryGroup[] = [
  {
    id: 'quarterly',
    title: 'Quarterly Reports',
    icon: '📊',
    reports: [
      {
        id: 'q_4',
        title: '4th Quarter Report',
        category: 'quarterly',
        defaultDeadline: '2026-01-31',
        reportNamePattern: '4th Quarter',
        quarterIdentifier: 'Fourth',
      },
      {
        id: 'q_1',
        title: '1st Quarter Report',
        category: 'quarterly',
        defaultDeadline: '2026-04-30',
        reportNamePattern: '1st Quarter',
        quarterIdentifier: 'First',
      },
      {
        id: 'q_2',
        title: '2nd Quarter Report',
        category: 'quarterly',
        defaultDeadline: '2026-07-31',
        reportNamePattern: '2nd Quarter',
        quarterIdentifier: 'Second',
      },
      {
        id: 'q_3',
        title: '3rd Quarter Report',
        category: 'quarterly',
        defaultDeadline: '2026-10-31',
        reportNamePattern: '3rd Quarter',
        quarterIdentifier: 'Third',
      },
    ],
  },
  {
    id: 'soi',
    title: 'Status of Implementations',
    icon: '📋',
    reports: [
      {
        id: 'soi_3',
        title: '3rd Quarter Status of Implementation Report',
        category: 'soi',
        defaultDeadline: '2026-01-31',
        reportNamePattern: '3rd Quarter SOI',
        quarterIdentifier: 'Third',
      },
      {
        id: 'soi_4',
        title: '4th Quarter Status of Implementation Report',
        category: 'soi',
        defaultDeadline: '2026-04-30',
        reportNamePattern: '4th Quarter SOI',
        quarterIdentifier: 'Fourth',
      },
      {
        id: 'soi_1',
        title: '1st Quarter Status of Implementation Report',
        category: 'soi',
        defaultDeadline: '2026-07-31',
        reportNamePattern: '1st Quarter SOI',
        quarterIdentifier: 'First',
      },
      {
        id: 'soi_2',
        title: '2nd Quarter Status of Implementation Report',
        category: 'soi',
        defaultDeadline: '2026-10-31',
        reportNamePattern: '2nd Quarter SOI',
        quarterIdentifier: 'Second',
      },
    ],
  },
  {
    id: 'ccc',
    title: 'CCC Review Reports',
    icon: '📑',
    reports: [
      {
        id: 'ccc_1',
        title: '1st Quarter CCC Review Report',
        category: 'ccc',
        defaultDeadline: '2026-04-10',
        reportNamePattern: '1st Quarter CCC',
        quarterIdentifier: 'First',
      },
      {
        id: 'ccc_2',
        title: '2nd Quarter CCC Review Report',
        category: 'ccc',
        defaultDeadline: '2026-07-10',
        reportNamePattern: '2nd Quarter CCC',
        quarterIdentifier: 'Second',
      },
      {
        id: 'ccc_3',
        title: '3rd Quarter CCC Review Report',
        category: 'ccc',
        defaultDeadline: '2026-10-10',
        reportNamePattern: '3rd Quarter CCC',
        quarterIdentifier: 'Third',
      },
      {
        id: 'ccc_4',
        title: '4th Quarter CCC Review Report',
        category: 'ccc',
        defaultDeadline: '2027-01-10',
        reportNamePattern: '4th Quarter CCC',
        quarterIdentifier: 'Fourth',
      },
    ],
  },
  {
    id: 'workplan',
    title: 'Annual Workplan',
    icon: '📅',
    reports: [
      {
        id: 'annual_workplan',
        title: 'Annual Workplan',
        category: 'annual',
        defaultDeadline: '2026-01-31',
        reportNamePattern: 'Workplan',
      },
    ],
  },
  {
    id: 'strategic',
    title: 'Strategic Plan',
    icon: '🎯',
    reports: [
      {
        id: 'strategic_plan',
        title: 'Strategic Plan',
        category: 'annual',
        defaultDeadline: '2026-01-31',
        reportNamePattern: 'Strategic Plan',
      },
    ],
  },
  {
    id: 'performance',
    title: 'Annual IA Performance Report',
    icon: '🏆',
    reports: [
      {
        id: 'annual_ia_perf',
        title: 'Annual IA Performance Report',
        category: 'annual',
        defaultDeadline: '2026-06-30',
        reportNamePattern: 'Performance',
      },
    ],
  },
];

type DeadlineConfig = {
  report: string;
  dateString: string;
};

function normalizeText(str?: string): string {
  if (!str) return '';
  return str.toLowerCase().replace(/[^a-z0-9]/g, '');
}

function matchesReport(submission: SubmissionRecord, report: ReportDefinition): boolean {
  if (submission.category && submission.category !== report.category) {
    return false;
  }

  const normRepName = normalizeText(submission.reportName);
  const normPeriod = normalizeText(submission.period);

  if (report.category === 'annual') {
    if (report.id === 'annual_workplan') {
      return normRepName.includes('workplan') || normPeriod.includes('workplan');
    }
    if (report.id === 'strategic_plan') {
      return normRepName.includes('strategic') || normPeriod.includes('strategic');
    }
    if (report.id === 'annual_ia_perf') {
      return (
        normRepName.includes('performance') ||
        normRepName.includes('internalaudit') ||
        normRepName.includes('statementonstatuses') ||
        normPeriod.includes('performance') ||
        normPeriod.includes('internalaudit')
      );
    }
  }

  // Quarterly / SOI / CCC
  const targetQuarter = report.quarterIdentifier?.toLowerCase() || '';
  const altQuarter = report.id.includes('1') ? '1st' :
                     report.id.includes('2') ? '2nd' :
                     report.id.includes('3') ? '3rd' : '4th';

  const matchesQuarter =
    (targetQuarter && (normPeriod.includes(targetQuarter) || normRepName.includes(targetQuarter))) ||
    normPeriod.includes(altQuarter) ||
    normRepName.includes(altQuarter);

  if (!matchesQuarter) return false;

  if (report.category === 'ccc') {
    return normRepName.includes('ccc') || submission.category === 'ccc';
  }
  if (report.category === 'soi') {
    return normRepName.includes('soi') || normRepName.includes('status') || submission.category === 'soi';
  }
  if (report.category === 'quarterly') {
    // Exclude SOI or CCC if marked in quarterly
    return !normRepName.includes('soi') && !normRepName.includes('ccc');
  }

  return true;
}

export default function DefaultersPage() {
  const [submissions, setSubmissions] = useState<SubmissionRecord[]>([]);
  const [configuredDeadlines, setConfiguredDeadlines] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  // Default collapsed headings
  const [expandedCategories, setExpandedCategories] = useState<Record<string, boolean>>({});
  const [expandedSubsets, setExpandedSubsets] = useState<Record<string, boolean>>({});

  // Sub-tabs for each subset: 'defaulters' | 'late' | 'ontime'
  const [activeTabs, setActiveTabs] = useState<Record<string, 'defaulters' | 'late' | 'ontime'>>({});
  const [searchTerms, setSearchTerms] = useState<Record<string, string>>({});
  const [typeFilters, setTypeFilters] = useState<Record<string, string>>({});
  const [regionFilters, setRegionFilters] = useState<Record<string, string>>({});

  // Master institutions list
  const allInstitutions = useMemo<MasterInstitution[]>(() => {
    const list: MasterInstitution[] = [];
    const seen = new Set<string>();

    // MDAs
    for (const mda of MDA_DATA) {
      const clean = mda.trim();
      const norm = normalizeText(clean);
      if (clean && !seen.has(norm)) {
        seen.add(norm);
        list.push({ name: clean, type: 'MDA' });
      }
    }

    // SOEs
    for (const soe of SOE_DATA) {
      const clean = soe.trim();
      const norm = normalizeText(clean);
      if (clean && !seen.has(norm)) {
        seen.add(norm);
        list.push({ name: clean, type: 'SOE' });
      }
    }

    // MMDAs
    for (const region of REGIONS) {
      const mmdaList = MMDA_DATA[region] || [];
      for (const mmda of mmdaList) {
        const clean = mmda.trim();
        const norm = normalizeText(clean);
        if (clean && !seen.has(norm)) {
          seen.add(norm);
          list.push({ name: clean, type: 'RCC/MMDA', region });
        }
      }
    }

    return list.sort((a, b) => a.name.localeCompare(b.name));
  }, []);

  const loadData = useCallback(async (isRefresh = false) => {
    if (isRefresh) setRefreshing(true);
    else setLoading(true);

    try {
      const [deadlinesData, subSnap] = await Promise.all([
        getCachedConfigDoc<{ json?: string }>('deadlines', isRefresh),
        getDocs(query(collection(db, 'submissions'), orderBy('timestamp', 'desc'))),
      ]);

      // Parse deadlines config if customized
      const deadMap: Record<string, string> = {};
      if (deadlinesData?.json) {
        try {
          const parsed = JSON.parse(deadlinesData.json) as DeadlineConfig[];
          if (Array.isArray(parsed)) {
            for (const d of parsed) {
              if (d.report && d.dateString) {
                deadMap[normalizeText(d.report)] = d.dateString;
              }
            }
          }
        } catch {
          // ignore parse errors
        }
      }
      setConfiguredDeadlines(deadMap);

      const records: SubmissionRecord[] = subSnap.docs.map((docSnap) => {
        const d = docSnap.data();
        let ts = 0;
        if (typeof d.timestamp === 'number') ts = d.timestamp;
        else if (d.timestamp?.toMillis) ts = d.timestamp.toMillis();
        else if (typeof d.timestamp === 'string') ts = new Date(d.timestamp).getTime();

        return {
          id: docSnap.id,
          institution: String(d.institution || ''),
          institutionType: String(d.institutionType || ''),
          category: String(d.category || d.type || ''),
          reportName: String(d.reportName || ''),
          period: String(d.period || ''),
          timestamp: ts || Date.now(),
          region: String(d.region || ''),
          senderName: String(d.senderName || ''),
          senderEmail: String(d.senderEmail || ''),
        };
      });

      setSubmissions(records);
    } catch (err) {
      console.error('Error loading defaulters data:', err);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Resolve deadline for a specific report
  const getResolvedDeadline = useCallback(
    (report: ReportDefinition): { dateString: string; deadlineDate: Date; isElapsed: boolean } => {
      // Check configured overrides
      const normTitle = normalizeText(report.title);
      let dateString = configuredDeadlines[normTitle] || report.defaultDeadline;

      // Ensure valid format
      if (!/^\d{4}-\d{2}-\d{2}$/.test(dateString)) {
        dateString = report.defaultDeadline;
      }

      // Deadline end of day: 23:59:59.999
      const deadlineDate = new Date(`${dateString}T23:59:59.999`);
      const now = new Date();
      const isElapsed = now.getTime() > deadlineDate.getTime();

      return { dateString, deadlineDate, isElapsed };
    },
    [configuredDeadlines]
  );

  // Calculate compliance data for each report definition
  const complianceData = useMemo(() => {
    const reportMap = new Map<
      string,
      {
        report: ReportDefinition;
        dateString: string;
        deadlineDate: Date;
        isElapsed: boolean;
        defaulters: Array<{
          institution: MasterInstitution;
          daysOverdue: number;
        }>;
        lateSubmissions: Array<{
          institution: MasterInstitution;
          submission: SubmissionRecord;
          daysLate: number;
        }>;
        onTimeSubmissions: Array<{
          institution: MasterInstitution;
          submission: SubmissionRecord;
        }>;
      }
    >();

    const now = new Date();

    for (const group of CATEGORY_GROUPS) {
      for (const rep of group.reports) {
        const { dateString, deadlineDate, isElapsed } = getResolvedDeadline(rep);

        const defaulters: Array<{ institution: MasterInstitution; daysOverdue: number }> = [];
        const lateSubmissions: Array<{
          institution: MasterInstitution;
          submission: SubmissionRecord;
          daysLate: number;
        }> = [];
        const onTimeSubmissions: Array<{
          institution: MasterInstitution;
          submission: SubmissionRecord;
        }> = [];

        // If deadline is not due yet within the year, do not calculate/show defaulters or late submissions
        if (isElapsed) {
          const deadlineMs = deadlineDate.getTime();
          const msPerDay = 1000 * 60 * 60 * 24;

          for (const inst of allInstitutions) {
            const normInst = normalizeText(inst.name);

            // Find matching submissions for this institution and report
            const matchingSubs = submissions.filter((s) => {
              const sNorm = normalizeText(s.institution);
              if (!sNorm || (sNorm !== normInst && !sNorm.includes(normInst) && !normInst.includes(sNorm))) {
                return false;
              }
              return matchesReport(s, rep);
            });

            if (matchingSubs.length > 0) {
              // Earliest submission for this report
              matchingSubs.sort((a, b) => a.timestamp - b.timestamp);
              const earliest = matchingSubs[0];

              if (earliest.timestamp <= deadlineMs) {
                onTimeSubmissions.push({ institution: inst, submission: earliest });
              } else {
                const daysLate = Math.max(1, Math.ceil((earliest.timestamp - deadlineMs) / msPerDay));
                lateSubmissions.push({ institution: inst, submission: earliest, daysLate });
              }
            } else {
              // No submission found -> DEFAULTER
              const daysOverdue = Math.max(1, Math.ceil((now.getTime() - deadlineMs) / msPerDay));
              defaulters.push({ institution: inst, daysOverdue });
            }
          }
        }

        reportMap.set(rep.id, {
          report: rep,
          dateString,
          deadlineDate,
          isElapsed,
          defaulters,
          lateSubmissions,
          onTimeSubmissions,
        });
      }
    }

    return reportMap;
  }, [allInstitutions, getResolvedDeadline, submissions]);

  // Overall totals across elapsed reports
  const summaryTotals = useMemo(() => {
    let totalDefaulters = 0;
    let totalLate = 0;
    let totalOnTime = 0;
    let totalElapsedReports = 0;

    for (const comp of complianceData.values()) {
      if (comp.isElapsed) {
        totalElapsedReports++;
        totalDefaulters += comp.defaulters.length;
        totalLate += comp.lateSubmissions.length;
        totalOnTime += comp.onTimeSubmissions.length;
      }
    }

    return { totalDefaulters, totalLate, totalOnTime, totalElapsedReports };
  }, [complianceData]);

  const toggleCategory = (catId: string) => {
    setExpandedCategories((prev) => ({
      ...prev,
      [catId]: !prev[catId],
    }));
  };

  const toggleSubset = (repId: string) => {
    setExpandedSubsets((prev) => ({
      ...prev,
      [repId]: !prev[repId],
    }));
  };

  // CSV Export Handler
  const handleExportCsv = (repId: string, title: string) => {
    const data = complianceData.get(repId);
    if (!data) return;

    const activeTab = activeTabs[repId] || 'defaulters';
    let rows: string[][] = [];
    let filename = '';

    if (activeTab === 'defaulters') {
      filename = `Defaulters_${title.replace(/\s+/g, '_')}.csv`;
      rows.push(['Institution Name', 'Type', 'Region', 'Deadline Date', 'Days Overdue', 'Status']);
      for (const d of data.defaulters) {
        rows.push([
          `"${d.institution.name.replace(/"/g, '""')}"`,
          d.institution.type,
          d.institution.region || 'N/A',
          data.dateString,
          d.daysOverdue.toString(),
          'Defaulter / Not Submitted',
        ]);
      }
    } else if (activeTab === 'late') {
      filename = `Late_Submissions_${title.replace(/\s+/g, '_')}.csv`;
      rows.push(['Institution Name', 'Type', 'Region', 'Deadline Date', 'Submitted Date', 'Days Late', 'Sender', 'Status']);
      for (const l of data.lateSubmissions) {
        const subDate = new Date(l.submission.timestamp).toLocaleDateString('en-US');
        rows.push([
          `"${l.institution.name.replace(/"/g, '""')}"`,
          l.institution.type,
          l.institution.region || 'N/A',
          data.dateString,
          subDate,
          l.daysLate.toString(),
          `"${l.submission.senderName || ''}"`,
          'Submitted Late',
        ]);
      }
    } else {
      filename = `OnTime_Submissions_${title.replace(/\s+/g, '_')}.csv`;
      rows.push(['Institution Name', 'Type', 'Region', 'Deadline Date', 'Submitted Date', 'Status']);
      for (const o of data.onTimeSubmissions) {
        const subDate = new Date(o.submission.timestamp).toLocaleDateString('en-US');
        rows.push([
          `"${o.institution.name.replace(/"/g, '""')}"`,
          o.institution.type,
          o.institution.region || 'N/A',
          data.dateString,
          subDate,
          'Submitted On Time',
        ]);
      }
    }

    const csvContent = 'data:text/csv;charset=utf-8,' + rows.map((e) => e.join(',')).join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', filename);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  if (loading) {
    return (
      <div className={styles.container}>
        <div className={styles.loadingWrapper}>
          <p>Analyzing submission compliance and compiling Defaulters&apos; List...</p>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <h1 className={styles.title}>
          <span className={styles.titleIcon}>⚠️</span>
          Defaulters&apos; List
        </h1>
        <p className={styles.subtitle}>
          Tracking institutions that have made late submissions or have not submitted mandatory statutory reports under the Public Financial Management Act and Regulations.
        </p>

        {/* Global Stats Overview */}
        <div className={styles.statsOverview}>
          <div className={styles.statCard}>
            <div className={styles.statValue}>{allInstitutions.length}</div>
            <div className={styles.statLabel}>Total Institutions Monitored</div>
          </div>
          <div className={styles.statCard}>
            <div className={styles.statValue}>{summaryTotals.totalElapsedReports}</div>
            <div className={styles.statLabel}>Elapsed Report Deadlines</div>
          </div>
          <div className={styles.statCard}>
            <div className={`${styles.statValue} ${styles.danger}`}>{summaryTotals.totalDefaulters}</div>
            <div className={styles.statLabel}>Outstanding Instances (Defaulters)</div>
          </div>
          <div className={styles.statCard}>
            <div className={`${styles.statValue} ${styles.warning}`}>{summaryTotals.totalLate}</div>
            <div className={styles.statLabel}>Late Submissions Recorded</div>
          </div>
        </div>

        {/* Action and Tips Bar */}
        <div className={styles.controlsBar}>
          <div className={styles.controlTip}>
            <span>ℹ️</span>
            <span>
              Click any category below to expand. Categories and subsets are collapsed by default. Reports whose deadlines have not yet elapsed do not show defaulters.
            </span>
          </div>
          <button
            type="button"
            className={styles.refreshBtn}
            onClick={() => loadData(true)}
            disabled={refreshing}
          >
            <span>🔄</span>
            <span>{refreshing ? 'Updating...' : 'Refresh Records'}</span>
          </button>
        </div>
      </header>

      {/* Main Categories Accordion Stack (Collapsed by Default) */}
      <div className={styles.categoriesStack}>
        {CATEGORY_GROUPS.map((group) => {
          const isCategoryExpanded = Boolean(expandedCategories[group.id]);

          // Category overview counts
          let groupDefaulters = 0;
          let groupLate = 0;
          let groupActiveCount = 0;

          for (const rep of group.reports) {
            const data = complianceData.get(rep.id);
            if (data?.isElapsed) {
              groupActiveCount++;
              groupDefaulters += data.defaulters.length;
              groupLate += data.lateSubmissions.length;
            }
          }

          return (
            <div
              key={group.id}
              className={`${styles.categoryCard} ${isCategoryExpanded ? styles.expanded : ''}`}
            >
              <button
                type="button"
                className={styles.categoryHeader}
                onClick={() => toggleCategory(group.id)}
                aria-expanded={isCategoryExpanded}
              >
                <div className={styles.categoryTitleGroup}>
                  <span className={styles.categoryIcon}>{group.icon}</span>
                  <span className={styles.categoryTitle}>{group.title}</span>
                  {groupActiveCount > 0 ? (
                    <span className={styles.categorySummaryBadge}>
                      {groupDefaulters} Defaulters • {groupLate} Late Submissions
                    </span>
                  ) : (
                    <span className={styles.categorySummaryBadge}>No elapsed deadlines yet</span>
                  )}
                </div>
                <span className={`${styles.categoryChevron} ${isCategoryExpanded ? styles.rotated : ''}`}>
                  ▼
                </span>
              </button>

              {isCategoryExpanded && (
                <div className={styles.categoryContent}>
                  <div className={styles.subsetsStack}>
                    {group.reports.map((rep) => {
                      const data = complianceData.get(rep.id);
                      if (!data) return null;

                      const isSubsetExpanded = Boolean(expandedSubsets[rep.id]);
                      const activeTab = activeTabs[rep.id] || 'defaulters';
                      const searchTerm = searchTerms[rep.id] || '';
                      const typeFilter = typeFilters[rep.id] || 'ALL';
                      const regionFilter = regionFilters[rep.id] || 'ALL';

                      // Format deadline date for display
                      const formattedDeadline = new Date(`${data.dateString}T00:00:00`).toLocaleDateString('en-US', {
                        month: 'long',
                        day: 'numeric',
                        year: 'numeric',
                      });

                      // Filter items for the active tab
                      let displayedItems: Array<{
                        name: string;
                        type: string;
                        region?: string;
                        days?: number;
                        submittedDate?: string;
                        sender?: string;
                      }> = [];

                      if (data.isElapsed) {
                        if (activeTab === 'defaulters') {
                          displayedItems = data.defaulters.map((d) => ({
                            name: d.institution.name,
                            type: d.institution.type,
                            region: d.institution.region,
                            days: d.daysOverdue,
                          }));
                        } else if (activeTab === 'late') {
                          displayedItems = data.lateSubmissions.map((l) => ({
                            name: l.institution.name,
                            type: l.institution.type,
                            region: l.institution.region,
                            days: l.daysLate,
                            submittedDate: new Date(l.submission.timestamp).toLocaleDateString('en-US', {
                              month: 'short',
                              day: 'numeric',
                              year: 'numeric',
                            }),
                            sender: l.submission.senderName,
                          }));
                        } else {
                          displayedItems = data.onTimeSubmissions.map((o) => ({
                            name: o.institution.name,
                            type: o.institution.type,
                            region: o.institution.region,
                            submittedDate: new Date(o.submission.timestamp).toLocaleDateString('en-US', {
                              month: 'short',
                              day: 'numeric',
                              year: 'numeric',
                            }),
                          }));
                        }

                        // Apply filters
                        if (typeFilter !== 'ALL') {
                          displayedItems = displayedItems.filter((item) => item.type === typeFilter);
                        }
                        if (regionFilter !== 'ALL') {
                          displayedItems = displayedItems.filter((item) => item.region === regionFilter);
                        }
                        if (searchTerm.trim()) {
                          const normQ = searchTerm.toLowerCase().trim();
                          displayedItems = displayedItems.filter((item) =>
                            item.name.toLowerCase().includes(normQ)
                          );
                        }
                      }

                      return (
                        <div key={rep.id} className={styles.subsetCard}>
                          <button
                            type="button"
                            className={styles.subsetHeader}
                            onClick={() => toggleSubset(rep.id)}
                            aria-expanded={isSubsetExpanded}
                          >
                            <div className={styles.subsetTitleGroup}>
                              <span className={styles.subsetTitle}>{rep.title}</span>
                              <span className={styles.deadlineTag}>Deadline: {formattedDeadline}</span>
                            </div>

                            <div className={styles.statusPills}>
                              {!data.isElapsed ? (
                                <span className={styles.pillUpcoming}>
                                  ⏳ Not Due Yet
                                </span>
                              ) : (
                                <>
                                  <span className={styles.pillDefaulter}>
                                    🔴 {data.defaulters.length} Defaulters
                                  </span>
                                  <span className={styles.pillLate}>
                                    🟡 {data.lateSubmissions.length} Late
                                  </span>
                                  <span className={styles.pillOnTime}>
                                    🟢 {data.onTimeSubmissions.length} On-Time
                                  </span>
                                </>
                              )}
                              <span className={`${styles.categoryChevron} ${isSubsetExpanded ? styles.rotated : ''}`}>
                                ▼
                              </span>
                            </div>
                          </button>

                          {isSubsetExpanded && (
                            <div className={styles.subsetBody}>
                              {!data.isElapsed ? (
                                <div className={styles.notDueNotice}>
                                  <span>⏳</span>
                                  <div>
                                    <strong>Submission date is not due yet.</strong>
                                    <p style={{ margin: '0.25rem 0 0 0', opacity: 0.9 }}>
                                      The deadline for <em>{rep.title}</em> is <strong>{formattedDeadline}</strong>. Defaulters and late submissions will not be shown until the deadline has elapsed.
                                    </p>
                                  </div>
                                </div>
                              ) : (
                                <>
                                  {/* Tab & Export Row */}
                                  <div className={styles.tabFilterRow}>
                                    <div className={styles.tabsGroup}>
                                      <button
                                        type="button"
                                        className={`${styles.tabBtn} ${activeTab === 'defaulters' ? styles.activeDefaulters : ''}`}
                                        onClick={() => setActiveTabs((prev) => ({ ...prev, [rep.id]: 'defaulters' }))}
                                      >
                                        <span>🔴 Defaulters</span>
                                        <span>({data.defaulters.length})</span>
                                      </button>
                                      <button
                                        type="button"
                                        className={`${styles.tabBtn} ${activeTab === 'late' ? styles.activeLate : ''}`}
                                        onClick={() => setActiveTabs((prev) => ({ ...prev, [rep.id]: 'late' }))}
                                      >
                                        <span>🟡 Late Submissions</span>
                                        <span>({data.lateSubmissions.length})</span>
                                      </button>
                                      <button
                                        type="button"
                                        className={`${styles.tabBtn} ${activeTab === 'ontime' ? styles.activeOnTime : ''}`}
                                        onClick={() => setActiveTabs((prev) => ({ ...prev, [rep.id]: 'ontime' }))}
                                      >
                                        <span>🟢 On-Time</span>
                                        <span>({data.onTimeSubmissions.length})</span>
                                      </button>
                                    </div>

                                    <button
                                      type="button"
                                      className={styles.exportBtn}
                                      onClick={() => handleExportCsv(rep.id, rep.title)}
                                      title="Download as CSV spreadsheet"
                                    >
                                      <span>📥</span>
                                      <span>Export CSV</span>
                                    </button>
                                  </div>

                                  {/* Filter Controls Bar */}
                                  <div className={styles.filterControls}>
                                    <input
                                      type="text"
                                      className={styles.searchInput}
                                      placeholder="Search institution name..."
                                      value={searchTerm}
                                      onChange={(e) =>
                                        setSearchTerms((prev) => ({ ...prev, [rep.id]: e.target.value }))
                                      }
                                    />

                                    <select
                                      className={styles.selectInput}
                                      value={typeFilter}
                                      onChange={(e) =>
                                        setTypeFilters((prev) => ({ ...prev, [rep.id]: e.target.value }))
                                      }
                                    >
                                      <option value="ALL">All Types</option>
                                      <option value="MDA">MDAs Only</option>
                                      <option value="SOE">SOEs Only</option>
                                      <option value="RCC/MMDA">RCCs / MMDAs Only</option>
                                    </select>

                                    {typeFilter !== 'MDA' && typeFilter !== 'SOE' && (
                                      <select
                                        className={styles.selectInput}
                                        value={regionFilter}
                                        onChange={(e) =>
                                          setRegionFilters((prev) => ({ ...prev, [rep.id]: e.target.value }))
                                        }
                                      >
                                        <option value="ALL">All Regions</option>
                                        {REGIONS.map((r) => (
                                          <option key={r} value={r}>
                                            {r}
                                          </option>
                                        ))}
                                      </select>
                                    )}
                                  </div>

                                  {/* Data Table */}
                                  {displayedItems.length === 0 ? (
                                    <div className={styles.emptyNotice}>
                                      {searchTerm || typeFilter !== 'ALL' || regionFilter !== 'ALL' ? (
                                        <p>No institutions matched the selected search or filters.</p>
                                      ) : activeTab === 'defaulters' ? (
                                        <p>🎉 Excellent! No institutions are defaulting on this report.</p>
                                      ) : activeTab === 'late' ? (
                                        <p>No late submissions recorded for this report.</p>
                                      ) : (
                                        <p>No on-time submissions found.</p>
                                      )}
                                    </div>
                                  ) : (
                                    <div className={styles.tableContainer}>
                                      <table className={styles.table}>
                                        <thead>
                                          <tr>
                                            <th>#</th>
                                            <th>Institution</th>
                                            <th>Type</th>
                                            <th>Region</th>
                                            {activeTab === 'defaulters' && <th>Overdue By</th>}
                                            {activeTab === 'late' && (
                                              <>
                                                <th>Submitted On</th>
                                                <th>Late By</th>
                                                <th>Sender</th>
                                              </>
                                            )}
                                            {activeTab === 'ontime' && <th>Submitted On</th>}
                                            <th>Status</th>
                                          </tr>
                                        </thead>
                                        <tbody>
                                          {displayedItems.map((item, index) => (
                                            <tr key={`${item.name}-${index}`}>
                                              <td>{index + 1}</td>
                                              <td>
                                                <strong>{item.name}</strong>
                                              </td>
                                              <td>
                                                <span
                                                  className={`${styles.typeBadge} ${
                                                    item.type === 'MDA'
                                                      ? styles.typeMda
                                                      : item.type === 'SOE'
                                                      ? styles.typeSoe
                                                      : styles.typeMmda
                                                  }`}
                                                >
                                                  {item.type}
                                                </span>
                                              </td>
                                              <td>{item.region || '—'}</td>
                                              {activeTab === 'defaulters' && (
                                                <td className={styles.overdueTag}>
                                                  {item.days} {item.days === 1 ? 'day' : 'days'}
                                                </td>
                                              )}
                                              {activeTab === 'late' && (
                                                <>
                                                  <td>{item.submittedDate || '—'}</td>
                                                  <td className={styles.lateTag}>
                                                    {item.days} {item.days === 1 ? 'day' : 'days'} late
                                                  </td>
                                                  <td>{item.sender || '—'}</td>
                                                </>
                                              )}
                                              {activeTab === 'ontime' && <td>{item.submittedDate || '—'}</td>}
                                              <td>
                                                {activeTab === 'defaulters' ? (
                                                  <span className={styles.pillDefaulter}>
                                                    Defaulter
                                                  </span>
                                                ) : activeTab === 'late' ? (
                                                  <span className={styles.pillLate}>
                                                    Submitted Late
                                                  </span>
                                                ) : (
                                                  <span className={styles.pillOnTime}>
                                                    Compliant
                                                  </span>
                                                )}
                                              </td>
                                            </tr>
                                          ))}
                                        </tbody>
                                      </table>
                                    </div>
                                  )}
                                </>
                              )}
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}

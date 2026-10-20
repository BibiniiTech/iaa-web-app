'use client';

import React, { useState, useEffect, useCallback, useRef } from 'react';
import { onAuthStateChanged } from 'firebase/auth';
import { auth } from '@/lib/firebase';
import styles from './findings.module.css';

const CATEGORIES = [
  { label: "All", value: null },
  { label: "Expenditure", value: "EXPENDITURE" },
  { label: "Revenue", value: "REVENUE" },
  { label: "Procurement", value: "PROCUREMENT/CONTRACT/PROJECT" },
  { label: "Stores", value: "STORES /LIBRARY" },
  { label: "Transport", value: "TRANSPORT" },
  { label: "IT", value: "IT" },
  { label: "Asset", value: "ASSET" },
  { label: "Payroll", value: "HUMAN RESOURCE / PAYROLL" },
  { label: "Tax", value: "TAX & STATUTORY DEDUCTIONS" },
  { label: "Reporting", value: "FINANCIAL REPORTING" },
  { label: "Administrative", value: "ADMINISTRATIVE / GOVERNANCE / OTHERS" }
];

export interface Finding {
  id: string;
  category: string;
  observation: string;
  criteria: string[];
  applicableTo: string;
}

const resolveLawKey = (lawString: string): string | null => {
  const cleanLaw = lawString.toLowerCase();
  let actName = null;
  if (cleanLaw.includes("pfm regulation") || cleanLaw.includes("pfmr") || cleanLaw.includes("l.i. 2378") || cleanLaw.includes("li 2378")) actName = "PFMR";
  else if (cleanLaw.includes("pfm act") || cleanLaw.includes("act 921")) actName = "PFM";
  else if (cleanLaw.includes("procurement") || cleanLaw.includes("ppa") || cleanLaw.includes("act 663") || cleanLaw.includes("act 914")) actName = "PPA";
  else if (cleanLaw.includes("local governance") || cleanLaw.includes("lga") || cleanLaw.includes("act 936")) actName = "LGA";
  else if (cleanLaw.includes("internal audit agency") || cleanLaw.includes("iaa") || cleanLaw.includes("act 658")) actName = "IAA";

  if (!actName) return null;

  const numberMatch = cleanLaw.match(/(?:section|regulation|reg|art|article|clause|l\.i\.?|li)\s*(\d+)/);
  const sectionNum = numberMatch ? numberMatch[1] : cleanLaw.match(/(\d+)/)?.[1];
  return sectionNum ? `${actName}_${sectionNum}` : null;
};

// Global cache for fetched law provisions during session
const lawCache = new Map<string, string>();

interface CriteriaItemProps {
  c: string;
  searchQuery: string;
  highlightSearchTerms: (text: string, query: string) => React.ReactNode;
}

function CriteriaItem({ c, searchQuery, highlightSearchTerms }: CriteriaItemProps) {
  const [isExpanded, setIsExpanded] = useState(false);
  const [detailText, setDetailText] = useState<string | null>(null);
  const [loadingLaw, setLoadingLaw] = useState(false);

  const key = resolveLawKey(c);

  const handleToggleExpand = async () => {
    const nextState = !isExpanded;
    setIsExpanded(nextState);

    if (nextState && key && !detailText) {
      if (lawCache.has(key)) {
        setDetailText(lawCache.get(key) || null);
        return;
      }

      setLoadingLaw(true);
      try {
        const token = await auth.currentUser?.getIdToken();
        const res = await fetch(`/api/findings/law?key=${encodeURIComponent(key)}`, {
          headers: token ? { Authorization: `Bearer ${token}` } : {},
        });
        if (res.ok) {
          const data = await res.json();
          const text = data.text || 'Full section text not found in database.';
          lawCache.set(key, text);
          setDetailText(text);
        } else {
          setDetailText('Unable to retrieve statutory provision at this time.');
        }
      } catch {
        setDetailText('Error loading statutory provision.');
      } finally {
        setLoadingLaw(false);
      }
    }
  };

  const handleCopyLaw = (e: React.MouseEvent) => {
    e.stopPropagation();
    const text = `${c}\n\n${detailText || "Full section text not loaded."}`;
    navigator.clipboard.writeText(text).then(() => {
      alert("Law reference copied to clipboard");
    });
  };

  return (
    <div
      className={styles.criteriaItem}
      onClick={handleToggleExpand}
    >
      <div className={styles.criteriaHeader}>
        <span className={styles.gavelIcon}>⚖️</span>
        <span className={styles.criteriaLabel}>{highlightSearchTerms(c, searchQuery)}</span>
        <button
          className={styles.copyIconBtn}
          onClick={handleCopyLaw}
          title="Copy law reference and text"
        >
          📋
        </button>
        <span className={`${styles.expandChevron} ${isExpanded ? styles.rotated : ''}`}>
          ▼
        </span>
      </div>
      {isExpanded && (
        <div className={styles.lawDetailContainer} onClick={e => e.stopPropagation()}>
          <p className={styles.lawDetailText}>
            {loadingLaw ? (
              <em>Loading provision details...</em>
            ) : (
              detailText || "Full section text not found in database. Search the web for context."
            )}
          </p>
        </div>
      )}
    </div>
  );
}

export default function FindingsPage() {
  const [searchQuery, setSearchQuery] = useState('');
  const [activeCategoryFilter, setActiveCategoryFilter] = useState<string | null>(null);
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [searchResults, setSearchResults] = useState<Finding[]>([]);
  const [totalCount, setTotalCount] = useState<number>(0);
  const [loading, setLoading] = useState<boolean>(false);
  const [isAuthenticated, setIsAuthenticated] = useState<boolean | null>(null);
  const [showCategoryMismatchPrompt, setShowCategoryMismatchPrompt] = useState<boolean>(false);

  const debounceTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (user) => {
      setIsAuthenticated(!!user);
    });
    return () => unsubscribe();
  }, []);

  const fetchFindings = useCallback(async (query: string, category: string | null) => {
    if (!query.trim() && !category) {
      setSearchResults([]);
      setTotalCount(0);
      setShowCategoryMismatchPrompt(false);
      setLoading(false);
      return;
    }

    setLoading(true);
    try {
      const token = await auth.currentUser?.getIdToken();
      const params = new URLSearchParams();
      if (query.trim()) params.set('q', query.trim());
      if (category) params.set('category', category);
      params.set('limit', '100');

      const res = await fetch(`/api/findings?${params.toString()}`, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });

      if (res.ok) {
        const data = await res.json();
        const findings: Finding[] = data.findings || [];
        setSearchResults(findings);
        setTotalCount(data.total || findings.length);

        // Check for category mismatch prompt if search has no results in category
        if (findings.length === 0 && category && query.trim()) {
          // Probe global search without category
          const probeParams = new URLSearchParams({ q: query.trim(), limit: '1' });
          const probeRes = await fetch(`/api/findings?${probeParams.toString()}`, {
            headers: token ? { Authorization: `Bearer ${token}` } : {},
          });
          if (probeRes.ok) {
            const probeData = await probeRes.json();
            setShowCategoryMismatchPrompt((probeData.total || 0) > 0);
          }
        } else {
          setShowCategoryMismatchPrompt(false);
        }
      } else {
        setSearchResults([]);
        setTotalCount(0);
      }
    } catch (err) {
      console.error('Error fetching findings:', err);
      setSearchResults([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (debounceTimeoutRef.current) {
      clearTimeout(debounceTimeoutRef.current);
    }

    debounceTimeoutRef.current = setTimeout(() => {
      fetchFindings(searchQuery, activeCategoryFilter);
    }, 250);

    return () => {
      if (debounceTimeoutRef.current) {
        clearTimeout(debounceTimeoutRef.current);
      }
    };
  }, [searchQuery, activeCategoryFilter, fetchFindings]);

  const highlightSearchTerms = (text: string, query: string) => {
    if (!query.trim()) return text;
    const terms = query.toLowerCase().split(/\s+/).filter(t => t.length > 2);
    terms.push(query.toLowerCase().trim());

    const lowerText = text.toLowerCase();
    const ranges: { start: number; end: number }[] = [];

    terms.forEach(term => {
      let idx = lowerText.indexOf(term);
      while (idx !== -1) {
        ranges.push({ start: idx, end: idx + term.length });
        idx = lowerText.indexOf(term, idx + 1);
      }
    });

    if (ranges.length === 0) return text;

    ranges.sort((a, b) => a.start - b.start);
    const merged: typeof ranges = [];
    let current = ranges[0];
    for (let i = 1; i < ranges.length; i++) {
      if (ranges[i].start < current.end) {
        current.end = Math.max(current.end, ranges[i].end);
      } else {
        merged.push(current);
        current = ranges[i];
      }
    }
    merged.push(current);

    const result: React.ReactNode[] = [];
    let lastIdx = 0;
    merged.forEach((r, i) => {
      result.push(text.substring(lastIdx, r.start));
      result.push(<mark key={i} className={styles.highlight}>{text.substring(r.start, r.end)}</mark>);
      lastIdx = r.end;
    });
    result.push(text.substring(lastIdx));
    return result;
  };

  return (
    <div className={styles.pageContainer}>
      <header className={styles.header}>
        <h1>Findings & Criteria</h1>
        <p className="text-muted">Search audit observations, criteria/laws, and covered entities to find relevant regulatory references.</p>
      </header>

      {isAuthenticated === false && (
        <div className={styles.onboardingState} style={{ marginBottom: '1.5rem', background: '#fef3c7', color: '#92400e', border: '1px solid #fde68a' }}>
          <p><strong>Note:</strong> Please sign in to authenticate and access the full statutory provisions and audit findings database.</p>
        </div>
      )}

      <section className={styles.searchSection}>
        <div className={styles.searchWrapper}>
          <input
            type="text"
            placeholder="Search keywords or phrase (e.g. imprest, voucher, VAT)"
            className={styles.searchInput}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
          {searchQuery && (
            <button className={styles.clearButton} onClick={() => setSearchQuery('')}>✕</button>
          )}
        </div>
        <div className={styles.filterChips}>
          {CATEGORIES.map(cat => (
            <button
              key={cat.label}
              className={`${styles.chip} ${activeCategoryFilter === cat.value ? styles.activeChip : ''}`}
              onClick={() => setActiveCategoryFilter(cat.value)}
            >
              {cat.label}
            </button>
          ))}
        </div>
      </section>

      <div className={styles.resultsList}>
        {loading ? (
          <div className={styles.onboardingState}>
            <p>Searching findings database...</p>
          </div>
        ) : !searchQuery.trim() && !activeCategoryFilter ? (
          <div className={styles.onboardingState}>
            <div className={styles.iconLarge}>✨</div>
            <p>Enter a search query or select a category chip to begin exploring findings database.</p>
          </div>
        ) : searchResults.length === 0 ? (
          <div className={styles.emptyState}>
            <p>No results found</p>
            {showCategoryMismatchPrompt && (
              <div className={styles.mismatchPrompt}>
                <p>Matches were found in other categories. Would you like to clear the category filter?</p>
                <button className={styles.outlineButton} onClick={() => setActiveCategoryFilter(null)}>
                  Clear Category Filter & Show All
                </button>
              </div>
            )}
          </div>
        ) : (
          <>
            <h2 className={styles.resultsCount}>Search Results ({totalCount})</h2>
            {searchResults.map(finding => (
              <div
                key={finding.id}
                className={`${styles.findingCard} ${expandedId === finding.id ? styles.expanded : ''}`}
                onClick={() => setExpandedId(expandedId === finding.id ? null : finding.id)}
              >
                <div className={styles.cardContent}>
                  <div className={styles.cardTop}>
                    <div style={{ flex: 1 }}>
                      <span className={styles.categoryTag}>{finding.category}</span>
                      <h3 className={styles.findingTitle}>
                        {highlightSearchTerms(finding.observation, searchQuery)}
                      </h3>
                    </div>
                  </div>

                  {expandedId === finding.id ? (
                    <div className={styles.cardBody} onClick={e => e.stopPropagation()}>
                      <hr className={styles.divider} />
                      <div className={styles.criteriaSection}>
                        <h4>Laws / Criteria Reference:</h4>
                        <div className={styles.criteriaList}>
                          {finding.criteria.map((c, i) => (
                            <CriteriaItem
                              key={i}
                              c={c}
                              searchQuery={searchQuery}
                              highlightSearchTerms={highlightSearchTerms}
                            />
                          ))}
                        </div>
                      </div>

                      {finding.applicableTo && (
                        <div className={styles.applicable}>
                          <strong>Applicable To:</strong> {finding.applicableTo}
                        </div>
                      )}

                      <div className={styles.collapseHint}>
                        <span>Show less</span>
                        <span>▲</span>
                      </div>
                    </div>
                  ) : (
                    <div className={styles.cardFooter}>
                      <span className={styles.viewCriteriaText}>
                        View {finding.criteria.length} {finding.criteria.length === 1 ? 'criteria' : 'criteria'} & applicability
                      </span>
                      <span className={styles.expandIcon}>▼</span>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </>
        )}
      </div>
    </div>
  );
}

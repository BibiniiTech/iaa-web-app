'use client';

import React, { useState, useMemo } from 'react';
import findingsData from '@/data/audit_findings.json';
import lawDetails from '@/data/law_details.json';
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

const STOP_WORDS = new Set(["in", "to", "of", "on", "and", "the", "for", "a", "an", "is", "are", "that", "this", "which", "with", "at", "by", "from", "or", "as"]);

const SYNONYMS: Record<string, string> = {
  "vat": "value added tax",
  "pfm": "public financial management",
  "pfmr": "public financial management regulations li 2378",
  "ppa": "public procurement act 663",
  "iaa": "internal audit agency",
  "gifmis": "ghana integrated financial management information system",
  "igf": "internally generated funds",
  "gcr": "general counterfoil receipt",
  "wht": "withholding tax",
  "paye": "pay as you earn",
  "act 921": "public financial management act",
  "li 2378": "public financial management regulations",
  "cash": "money funds currency cheque bank float imprest",
  "money": "cash funds currency cheque bank float imprest",
  "funds": "cash money currency bank capital float imprest",
  "fund": "cash money currency bank capital float imprest",
  "payment": "disbursement voucher cheque pv",
  "disbursement": "payment voucher cheque pv",
  "voucher": "pv payment receipt invoice document",
  "pv": "voucher payment disbursement",
  "invoice": "receipt bill voucher document",
  "receipt": "gcr invoice voucher bill payment",
  "imprest": "petty cash float advance funds",
  "advance": "imprest float petty cash funds",
  "revenue": "igf income tax collection receipt collection",
  "income": "revenue igf collection",
  "stores": "inventory stock items goods warehouse",
  "stock": "stores inventory items goods warehouse",
  "inventory": "stores stock items goods warehouse",
  "goods": "items stock inventory stores asset asset",
  "purchases": "procurement goods contract tender ppa",
  "contract": "agreement procurement project tender ppa",
  "procurement": "purchases contract tender ppa buy",
  "tax": "gra wht paye levy duty tariff statutory",
  "salary": "payroll wage paye ssnit compensation staff employee",
  "payroll": "salary wage paye ssnit compensation staff employee",
  "wage": "salary payroll ssnit compensation staff employee",
  "staff": "employee human resource personnel hr worker officers",
  "employee": "staff human resource personnel hr worker officer",
  "personnel": "staff employee human resource hr worker officer",
  "vehicle": "transport car automobile fuel logbook transport",
  "car": "vehicle transport fuel logbook transport",
  "transport": "vehicle car fuel logbook transit",
  "fuel": "petrol diesel vehicle car transport coupon logbook",
  "fictitious": "fake forged fabricated false dummy simulated phantom non-existent",
  "fake": "fictitious forged fabricated false dummy simulated",
  "forged": "fictitious fake fabricated false copy altered simulated",
  "suspicious": "dubious questionable fishy anomalous irregular unusual",
  "irregular": "suspicious dubious questionable fishy anomalous unusual exception non-compliant",
  "fraudulent": "dishonest corrupt illegal misappropriated unauthorized unapproved stolen missing theft",
  "corrupt": "fraudulent dishonest illegal misappropriated bribe kickback",
  "illegal": "unlawful non-compliant unauthorized unapproved prohibited fraudulent corrupt",
  "unauthorized": "unapproved non-compliant illegal unlawful disallowed",
  "missing": "lost stolen short count shortage unaccounted theft",
  "stolen": "missing lost shortage misappropriated theft"
};

interface Finding {
  id: string;
  category: string;
  observation: string;
  criteria: string[];
  applicableTo: string;
}

const levenshteinDistance = (s1: string, s2: string): number => {
  const d: number[][] = Array.from({ length: s1.length + 1 }, () => Array(s2.length + 1).fill(0));
  for (let i = 0; i <= s1.length; i++) d[i][0] = i;
  for (let j = 0; j <= s2.length; j++) d[0][j] = j;
  for (let i = 1; i <= s1.length; i++) {
    for (let j = 1; j <= s2.length; j++) {
      const cost = s1[i - 1] === s2[j - 1] ? 0 : 1;
      d[i][j] = Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost);
    }
  }
  return d[s1.length][s2.length];
};

const isFuzzyMatch = (word: string, target: string): boolean => {
  if (word.length < 4 || target.length < 4) return word === target;
  return levenshteinDistance(word, target) <= 1;
};

const expandQueryWithSynonyms = (query: string): string => {
  let expanded = query;
  const words = query.toLowerCase().split(/\W+/).filter(Boolean);
  Object.entries(SYNONYMS).forEach(([abbr, full]) => {
    const isMatch = abbr.includes(" ") ? query.toLowerCase().includes(abbr) : words.includes(abbr);
    if (isMatch) expanded += ` ${full}`;
  });
  return expanded;
};

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

export default function FindingsPage() {
  const [searchQuery, setSearchQuery] = useState('');
  const [activeCategoryFilter, setActiveCategoryFilter] = useState<string | null>(null);
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [activeLawDialog, setActiveLawDialog] = useState<{ label: string, detail: string | null } | null>(null);

  const findings = findingsData as Finding[];

  // IDF weights for search
  const wordWeights = useMemo(() => {
    const counts: Record<string, number> = {};
    findings.forEach(finding => {
      const words = new Set((finding.observation + " " + finding.criteria.join(" ")).toLowerCase().split(/\W+/).filter(w => w.length > 2));
      words.forEach(w => { counts[w] = (counts[w] || 0) + 1; });
    });
    const total = findings.length;
    const weights: Record<string, number> = {};
    Object.entries(counts).forEach(([word, count]) => {
      weights[word] = Math.log(total / count) + 1;
    });
    return weights;
  }, [findings]);

  const { searchResults, showCategoryMismatchPrompt } = useMemo(() => {
    const filteredDatabase = activeCategoryFilter ? findings.filter(f => f.category === activeCategoryFilter) : findings;

    if (!searchQuery.trim()) {
      return {
        searchResults: activeCategoryFilter ? filteredDatabase.slice(0, 150) : [],
        showCategoryMismatchPrompt: false
      };
    }

    const rawQuery = searchQuery.toLowerCase().trim();
    const expandedQuery = expandQueryWithSynonyms(rawQuery);
    const queryTerms = expandedQuery.split(/\W+/).filter(t => t.length > 0 && !STOP_WORDS.has(t));

    const getScore = (finding: Finding) => {
      let score = 0;
      const obsLower = finding.observation.toLowerCase();
      const critLower = finding.criteria.join(" ").toLowerCase();
      const obsWords = obsLower.split(/\W+/).filter(Boolean);
      const critWords = critLower.split(/\W+/).filter(Boolean);

      if (obsLower.includes(rawQuery)) score += 50;
      if (critLower.includes(rawQuery)) score += 30;

      queryTerms.forEach(term => {
        const weight = wordWeights[term] || 1.0;
        const isWholeWordObs = obsWords.includes(term);
        const isWholeWordCrit = critWords.includes(term);

        if (isWholeWordObs) {
          score += 15 * weight;
          if (obsLower.startsWith(term) || obsLower.includes(` ${term}`)) score += 5;
        } else if (obsLower.includes(term)) {
          if (term.length >= 4) score += 5 * weight;
        } else if (obsWords.some(w => isFuzzyMatch(w, term))) {
          score += 5 * weight;
        }

        if (isWholeWordCrit) {
          score += 10 * weight;
        } else if (critLower.includes(term) && term.length >= 4) {
          score += 4 * weight;
        }
      });
      return score;
    };

    const searchInDb = (db: Finding[]) => {
      if (queryTerms.length === 0) {
        return db.filter(f => f.observation.toLowerCase().includes(rawQuery) || f.criteria.some(c => c.toLowerCase().includes(rawQuery))).slice(0, 100);
      }
      return db
        .map(f => ({ finding: f, score: getScore(f) }))
        .filter(x => x.score > 0)
        .sort((a, b) => b.score - a.score)
        .map(x => x.finding)
        .slice(0, 100);
    };

    const categoryResults = searchInDb(filteredDatabase);
    const mismatch = categoryResults.length === 0 && activeCategoryFilter && searchInDb(findings).length > 0;

    return {
      searchResults: categoryResults,
      showCategoryMismatchPrompt: !!mismatch
    };
  }, [searchQuery, activeCategoryFilter, findings, wordWeights]);

  const handleCopy = (finding: Finding) => {
    const text = `Observation: ${finding.observation}\nCriteria: ${finding.criteria.join(", ")}`;
    navigator.clipboard.writeText(text).then(() => {
      alert("Copied to clipboard");
    });
  };

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
        {!searchQuery.trim() && !activeCategoryFilter ? (
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
            <h2 className={styles.resultsCount}>Search Results ({searchResults.length})</h2>
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
                    <button
                      className={styles.copyButton}
                      onClick={(e) => { e.stopPropagation(); handleCopy(finding); }}
                      title="Copy finding"
                    >
                      📋
                    </button>
                  </div>

                  {expandedId === finding.id ? (
                    <div className={styles.cardBody} onClick={e => e.stopPropagation()}>
                      <hr className={styles.divider} />
                      <div className={styles.criteriaSection}>
                        <h4>Laws / Criteria Reference:</h4>
                        <div className={styles.criteriaList}>
                          {finding.criteria.map((c, i) => (
                            <div
                              key={i}
                              className={styles.criteriaItem}
                              onClick={() => {
                                const key = resolveLawKey(c);
                                setActiveLawDialog({
                                  label: c,
                                  detail: key ? (lawDetails as any)[key] : null
                                });
                              }}
                            >
                              <span className={styles.gavelIcon}>⚖️</span>
                              <span className={styles.criteriaLabel}>{highlightSearchTerms(c, searchQuery)}</span>
                              <span className={styles.openIcon}>↗</span>
                            </div>
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

      {activeLawDialog && (
        <div className={styles.modalOverlay} onClick={() => setActiveLawDialog(null)}>
          <div className={styles.modalContent} onClick={e => e.stopPropagation()}>
            <h3>Applicable Law Details</h3>
            <div className={styles.modalBody}>
              <p className={styles.lawTitle}>{activeLawDialog.label}</p>
              <hr />
              <p className={styles.lawDetail}>
                {activeLawDialog.detail || "Full section text not found in database. Search the web for context."}
              </p>
            </div>
            <div className={styles.modalActions}>
              <button
                className={styles.searchWebButton}
                onClick={() => {
                  const q = activeLawDialog.detail || activeLawDialog.label;
                  window.open(`https://www.google.com/search?q=${encodeURIComponent(q)}`, '_blank');
                }}
              >
                Search Web
              </button>
              <button className={styles.closeButton} onClick={() => setActiveLawDialog(null)}>Close</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

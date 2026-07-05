'use client';

import React, { useState, useEffect, useMemo, Suspense } from 'react';
import { collection, query, where, orderBy, onSnapshot, getDocs } from 'firebase/firestore';
import { db } from '@/lib/firebase';
import { useSearchParams } from 'next/navigation';
import styles from './resources.module.css';

interface DbDocument {
  id: string;
  name: string;
  category: string;
  downloadUrl: string;
  timestamp: number;
}

interface ChecklistItem {
  text: string;
  checked: boolean;
}

export default function ResourcesPage() {
  return (
    <Suspense fallback={<div className={styles.emptyText}>Loading Resources...</div>}>
      <ResourcesPageContent />
    </Suspense>
  );
}

function ResourcesPageContent() {
  const searchParams = useSearchParams();
  const initialExpand = searchParams.get('expand');

  const [expandedCategories, setExpandedCategories] = useState<Record<string, boolean>>({
    legislations: initialExpand === 'legislations',
    guidelines: initialExpand === 'guidelines',
    templates: initialExpand === 'templates',
    checklists: initialExpand === 'checklists',
    others: initialExpand === 'others',
  });

  const [documents, setDocuments] = useState<DbDocument[]>([]);
  const [loading, setLoading] = useState(true);

  // Interactive Checklist State
  const [selectedPlaybook, setSelectedPlaybook] = useState("Pre-Audit Planning");

  const initialChecklists = {
    "Pre-Audit Planning": [
      { text: "Establish audit objective and scope", checked: false },
      { text: "Request background financial documentation", checked: false },
      { text: "Assess risk landscape & initial compliance criteria", checked: false },
      { text: "Assign audit team roles and responsibilities", checked: false },
      { text: "Schedule entry conference with auditee", checked: false }
    ],
    "Fieldwork Walk-Through": [
      { text: "Conduct walk-through of transaction controls", checked: false },
      { text: "Select sample vouchers and documentation", checked: false },
      { text: "Test adherence to PFM Act (Act 921) rules", checked: false },
      { text: "Document control exceptions and errors", checked: false },
      { text: "Discuss findings with departmental heads", checked: false }
    ],
    "Quality Assurance Review": [
      { text: "Review audit working papers against standards", checked: false },
      { text: "Verify criteria source matches PFM guidelines", checked: false },
      { text: "Confirm evidence supports each exception finding", checked: false },
      { text: "Format draft audit report for review", checked: false },
      { text: "Obtain sign-off from Head of Audit", checked: false }
    ]
  };

  const [checklists, setChecklists] = useState(initialChecklists);

  useEffect(() => {
    // Listen for global sync or just fetch once
    // To match Android's sync behavior, we listen to portal_documents
    const q = query(collection(db, "portal_documents"), orderBy("timestamp", "desc"));

    const unsubscribe = onSnapshot(q, (snapshot) => {
      const docsData = snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
      })) as DbDocument[];
      setDocuments(docsData);
      setLoading(false);
    }, (error) => {
      console.error("Error fetching documents:", error);
      setLoading(false);
    });

    return () => unsubscribe();
  }, []);

  const getDocsByCategory = (category: string) => {
    const filtered = documents.filter(doc => doc.category === category);

    if (category === 'others') {
      // For "Other Resources", sort by timestamp descending (newest first)
      return filtered.sort((a, b) => b.timestamp - a.timestamp);
    }

    return filtered.sort((a, b) => a.name.toLowerCase().localeCompare(b.name.toLowerCase()));
  };

  const formatName = (name: string) => {
    let clean = name.replace(/\.(pdf|docx?|xlsx?|pptx?|txt|zip)$/i, '');
    clean = clean.replace(/[_-]/g, ' ');
    const acronyms = ['PFM', 'IAA', 'LGA', 'PPA', 'CCC', 'NACAP', 'LI', 'ACT'];

    return clean.split(' ').map(word => {
      const upper = word.toUpperCase();
      if (acronyms.includes(upper)) return upper;
      const smallWords = ['and', 'of', 'for', 'the', 'in', 'on', 'with'];
      if (smallWords.includes(word.toLowerCase())) return word.toLowerCase();
      return word.charAt(0).toUpperCase() + word.slice(1).toLowerCase();
    }).join(' ');
  };

  const isNew = (timestamp: number) => {
    const weekAgo = Date.now() - (7 * 24 * 60 * 60 * 1000);
    return timestamp > weekAgo;
  };

  const toggleCategory = (category: string) => {
    setExpandedCategories(prev => ({
      ...prev,
      [category]: !prev[category]
    }));
  };

  const toggleChecklistItem = (playbook: string, index: number) => {
    setChecklists(prev => {
      const newItems = [...prev[playbook as keyof typeof initialChecklists]];
      newItems[index] = { ...newItems[index], checked: !newItems[index].checked };
      return { ...prev, [playbook]: newItems };
    });
  };

  const categories = [
    {
      id: 'legislations',
      title: 'Legislations',
      subtitle: 'Acts, Regulations, L.I.s, etc.',
      icon: (
        <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
          <path d="M5.22 8.57l3.53 3.53 4.24-4.24L9.46 4.33a1.5 1.5 0 0 0-2.12 0L5.22 6.45a1.5 1.5 0 0 0 0 2.12zM16.54 11l-4.24 4.24 3.53 3.53a1.5 1.5 0 0 0 2.12 0l2.12-2.12a1.5 1.5 0 0 0 0-2.12L16.54 11zM10.5 15l-1.41-1.41L2.12 20.5a1 1 0 0 0 0 1.41l.71.71a1 1 0 0 0 1.41 0L10.5 15z"/>
        </svg>
      ),
      emptyText: 'No legislations available'
    },
    {
      id: 'guidelines',
      title: 'Guidelines & Manuals',
      subtitle: 'Standard Procedures, Directives and Operational Manuals.',
      icon: (
        <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
          <path d="M16.5 6V4H15V2h-2v2H5c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V6h-4.5zM9 18H7v-2h2v2zm0-4H7v-2h2v2zm0-4H7V8h2v2zm10 8h-8v-2h8v2zm0-4h-8v-2h8v2zm0-4h-8V8h8v2z"/>
        </svg>
      ),
      emptyText: 'No guidelines available'
    },
    {
      id: 'templates',
      title: 'Reporting Templates',
      subtitle: 'Standard formats for various reports.',
      icon: (
        <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
          <path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/>
        </svg>
      ),
      emptyText: 'No templates available'
    },
    {
      id: 'checklists',
      title: 'Checklists & Walk-Throughs',
      subtitle: 'Audit step checklists and templates.',
      icon: (
        <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
          <path d="M22 7h-9v2h9V7zm0 8h-9v2h9v-2zM2 7h9v2H2V7zm0 8h9v2H2v-2zM7.19 22.19L2.34 17.34l1.41-1.41 3.44 3.44 7.44-7.44 1.41 1.41-8.85 8.85z"/>
        </svg>
      ),
      emptyText: 'No downloadable checklists available'
    },
    {
      id: 'others',
      title: 'Other Resources',
      subtitle: 'Additional audit materials and miscellaneous files.',
      icon: (
        <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
          <path d="M6 10c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm12 0c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm-6 0c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"/>
        </svg>
      ),
      emptyText: 'No additional resources available.'
    }
  ];

  return (
    <div className={styles.pageContainer}>
      <header className={styles.header}>
        <h1>Resources Portal</h1>
        <p>Access laws, guidelines, and templates to support your internal audit functions.</p>
      </header>

      <div className={styles.categoryList}>
        {categories.map((cat) => (
          <div key={cat.id} className={styles.categoryCard}>
            <div
              className={styles.categoryHeader}
              onClick={() => toggleCategory(cat.id)}
            >
              <div className={styles.iconWrapper}>
                {cat.icon}
              </div>
              <div className={styles.categoryInfo}>
                <h2>{cat.title}</h2>
                <p>{cat.subtitle}</p>
              </div>
              <div className={styles.expandIcon} style={{ transform: expandedCategories[cat.id] ? 'rotate(180deg)' : 'none' }}>
                ▼
              </div>
            </div>

            {expandedCategories[cat.id] && (
              <div className={styles.categoryContent}>
                {cat.id === 'checklists' && (
                  <div className={styles.interactiveSection}>
                    <h3 className={styles.playbookTitle}>Interactive Audit Playbooks</h3>
                    <select
                      className={styles.dropdown}
                      value={selectedPlaybook}
                      onChange={(e) => setSelectedPlaybook(e.target.value)}
                    >
                      {Object.keys(checklists).map(p => (
                        <option key={p} value={p}>{p}</option>
                      ))}
                    </select>

                    <div className={styles.checklistCard}>
                      {checklists[selectedPlaybook as keyof typeof initialChecklists].map((item, idx) => (
                        <div
                          key={idx}
                          className={styles.checklistItem}
                          onClick={() => toggleChecklistItem(selectedPlaybook, idx)}
                        >
                          <input
                            type="checkbox"
                            className={styles.checkbox}
                            checked={item.checked}
                            readOnly
                          />
                          <span className={`${styles.itemText} ${item.checked ? styles.itemTextChecked : ''}`}>
                            {item.text}
                          </span>
                        </div>
                      ))}
                    </div>

                    <div className={styles.divider}></div>
                    <h4 className={styles.sectionSubtitle}>Downloadable Checklists & Manuals</h4>
                  </div>
                )}

                <div className={styles.docList}>
                  {getDocsByCategory(cat.id).length === 0 ? (
                    <p className={styles.emptyText}>{cat.emptyText}</p>
                  ) : (
                    getDocsByCategory(cat.id).map((doc) => (
                      <a
                        key={doc.id}
                        href={doc.downloadUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className={styles.docItem}
                      >
                        <span className={styles.docIcon}>📄</span>
                        <div className={styles.docInfo}>
                          <span className={styles.docName}>{formatName(doc.name)}</span>
                          {cat.id === 'others' && (
                            <span className={styles.docMeta}>
                              Added {new Date(doc.timestamp).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })}
                            </span>
                          )}
                        </div>
                        {isNew(doc.timestamp) && <span className={styles.newBadge}>NEW</span>}
                        <span className={styles.openIcon}>↗️</span>
                      </a>
                    ))
                  )}
                </div>
              </div>
            )}
          </div>
        ))}
      </div>

      <div style={{ height: '100px' }}></div>
    </div>
  );
}

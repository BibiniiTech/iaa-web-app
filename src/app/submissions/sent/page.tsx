'use client';

import React, { useState, useEffect, useMemo } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { collection, getDocs, query, orderBy, where, limit } from 'firebase/firestore';
import { auth, db } from '@/lib/firebase';
import styles from '../submissions.module.css';

interface SentReportItem {
  id: string;
  reportName: string;
  category: string;
  institution: string;
  senderName: string;
  senderEmail: string;
  timestamp: number;
  fileNames: string[];
  downloadUrls: string[];
}

export default function SentReportsPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [institution, setInstitution] = useState('');
  const [reports, setReports] = useState<SentReportItem[]>([]);
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    async function loadSentReports() {
      setLoading(true);
      try {
        let userInst = '';
        // Check demo user
        if (typeof window !== 'undefined') {
          const demoStr = sessionStorage.getItem('iaa_demo_user');
          if (demoStr) {
            try {
              const demo = JSON.parse(demoStr);
              userInst = demo.institution || '';
            } catch {}
          }
        }

        // Check firebase auth user doc
        if (!userInst && auth.currentUser) {
          const userDoc = await getDocs(query(collection(db, 'users'), where('uid', '==', auth.currentUser.uid)));
          if (!userDoc.empty) {
            const data = userDoc.docs[0].data();
            userInst = data.institution || data.institutionName || '';
          }
        }

        setInstitution(userInst);

        // Fetch submissions
        let fetchedSubmissions: SentReportItem[] = [];
        try {
          const subSnap = await getDocs(query(collection(db, 'submissions'), orderBy('timestamp', 'desc'), limit(500)));
          const targetNorm = userInst.toLowerCase().replace(/[^a-z0-9]/g, '');

          fetchedSubmissions = subSnap.docs
            .map((docSnap) => {
              const d = docSnap.data();
              let ts = 0;
              if (typeof d.timestamp === 'number') ts = d.timestamp;
              else if (d.timestamp?.toMillis) ts = d.timestamp.toMillis();
              else if (typeof d.timestamp === 'string') ts = new Date(d.timestamp).getTime();

              const fileNames = Array.isArray(d.fileNames) ? d.fileNames : [];
              const downloadUrls = Array.isArray(d.downloadUrls) ? d.downloadUrls : Array.isArray(d.fileUrls) ? d.fileUrls : [];

              return {
                id: docSnap.id,
                reportName: String(d.reportName || d.period || 'Statutory Report'),
                category: String(d.category || 'other'),
                institution: String(d.institution || ''),
                senderName: String(d.senderName || 'Internal Auditor'),
                senderEmail: String(d.senderEmail || ''),
                timestamp: ts || Date.now(),
                fileNames,
                downloadUrls,
              };
            })
            .filter((sub) => {
              if (!targetNorm) return true; // If user has no institution, show all for audit/demo
              const subNorm = sub.institution.toLowerCase().replace(/[^a-z0-9]/g, '');
              return subNorm === targetNorm || subNorm.includes(targetNorm) || targetNorm.includes(subNorm);
            });
        } catch (e) {
          console.error('Error querying submissions:', e);
        }

        setReports(fetchedSubmissions);
      } finally {
        setLoading(false);
      }
    }

    loadSentReports();
  }, []);

  const filteredReports = useMemo(() => {
    if (!searchQuery.trim()) return reports;
    const q = searchQuery.toLowerCase().trim();
    return reports.filter(
      (r) =>
        r.reportName.toLowerCase().includes(q) ||
        r.category.toLowerCase().includes(q) ||
        r.senderName.toLowerCase().includes(q) ||
        r.fileNames.some((f) => f.toLowerCase().includes(q))
    );
  }, [reports, searchQuery]);

  const formatDate = (ts: number) => {
    if (!ts) return 'N/A';
    const d = new Date(ts);
    return d.toLocaleDateString('en-GB', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const getCategoryBadgeClass = (category: string) => {
    switch (category.toLowerCase()) {
      case 'quarterly':
        return 'Quarterly IA Report';
      case 'annual':
        return 'Annual Report';
      case 'ccc':
        return 'CCC Review Report';
      case 'soi':
        return 'Status of Implementation';
      default:
        return 'Special / Other Report';
    }
  };

  return (
    <div className={styles.container}>
      <div style={{ marginBottom: '1.5rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <button
          onClick={() => router.push('/submissions')}
          style={{
            background: 'none',
            border: 'none',
            color: 'var(--primary)',
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            fontSize: '0.95rem',
            fontWeight: 600,
            cursor: 'pointer',
          }}
        >
          ← Back to Submissions Portal
        </button>
      </div>

      <header style={{ textAlign: 'center', marginBottom: '2rem' }}>
        <h1 className={styles.title}>Sent Reports Archive</h1>
        <p className={styles.description}>
          Downloadable repository of all statutory and special reports submitted by internal auditors from{' '}
          <strong>{institution || 'your institution'}</strong>.
        </p>
      </header>

      {/* Institution Banner */}
      <div
        style={{
          background: 'var(--card-bg)',
          border: '1px solid var(--border-color)',
          borderRadius: '12px',
          padding: '1rem 1.25rem',
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          marginBottom: '1.5rem',
          boxShadow: '0 2px 8px rgba(0,0,0,0.03)',
        }}
      >
        <div style={{ fontSize: '1.75rem' }}>🏛️</div>
        <div>
          <div style={{ fontWeight: 700, color: 'var(--text-primary)', fontSize: '1rem' }}>
            {institution || 'All Institution Submissions'}
          </div>
          <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
            All colleagues within this entity have access to download and review copies of these submissions.
          </div>
        </div>
      </div>

      {/* Search Input */}
      <div style={{ marginBottom: '1.5rem' }}>
        <input
          type="text"
          className={styles.inputField}
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          placeholder="Search by report name, category, or file name..."
        />
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-secondary)' }}>
          Loading sent reports…
        </div>
      ) : filteredReports.length === 0 ? (
        <div
          style={{
            textAlign: 'center',
            padding: '3.5rem 1.5rem',
            background: 'var(--card-bg)',
            borderRadius: '16px',
            border: '1px dashed var(--border-color)',
          }}
        >
          <div style={{ fontSize: '2.5rem', marginBottom: '1rem' }}>📂</div>
          <h3 style={{ fontSize: '1.2rem', marginBottom: '0.5rem', color: 'var(--text-primary)' }}>
            {searchQuery ? 'No matching reports found' : 'No sent reports recorded yet'}
          </h3>
          <p style={{ color: 'var(--text-secondary)', maxWidth: '480px', margin: '0 auto', fontSize: '0.9rem' }}>
            {searchQuery
              ? 'Try adjusting your search criteria.'
              : 'Reports filed by any internal auditor from your institution will automatically appear here with downloadable files.'}
          </p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          {filteredReports.map((report) => (
            <div
              key={report.id}
              style={{
                background: 'var(--card-bg)',
                border: '1px solid var(--border-color)',
                borderRadius: '12px',
                padding: '1.25rem',
                boxShadow: '0 2px 8px rgba(0,0,0,0.03)',
                display: 'flex',
                flexDirection: 'column',
                gap: '0.75rem',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '8px' }}>
                <span
                  style={{
                    background: 'var(--primary-light, #e0f2fe)',
                    color: 'var(--primary, #0369a1)',
                    fontWeight: 600,
                    fontSize: '0.75rem',
                    padding: '3px 8px',
                    borderRadius: '4px',
                  }}
                >
                  {getCategoryBadgeClass(report.category)}
                </span>
                <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                  {formatDate(report.timestamp)}
                </span>
              </div>

              <div>
                <h3 style={{ margin: '0 0 4px 0', fontSize: '1.1rem', color: 'var(--text-primary)' }}>
                  {report.reportName}
                </h3>
                <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                  Submitted by: <strong>{report.senderName}</strong> {report.senderEmail && `(${report.senderEmail})`}
                </div>
              </div>

              {report.fileNames.length > 0 && (
                <div style={{ borderTop: '1px dashed var(--border-color)', paddingTop: '0.75rem', marginTop: '0.25rem' }}>
                  <div style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--primary)', marginBottom: '6px' }}>
                    Attached Documents ({report.fileNames.length}):
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                    {report.fileNames.map((fileName, idx) => {
                      const url = report.downloadUrls[idx];
                      return (
                        <div
                          key={`${fileName}-${idx}`}
                          style={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                            background: 'var(--bg-surface, #f8fafc)',
                            padding: '6px 12px',
                            borderRadius: '6px',
                            fontSize: '0.85rem',
                          }}
                        >
                          <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', marginRight: '8px' }}>
                            📄 {fileName}
                          </span>
                          {url ? (
                            <a
                              href={url}
                              target="_blank"
                              rel="noopener noreferrer"
                              style={{
                                color: 'white',
                                background: 'var(--primary)',
                                textDecoration: 'none',
                                fontSize: '0.75rem',
                                padding: '4px 10px',
                                borderRadius: '4px',
                                fontWeight: 600,
                                flexShrink: 0,
                              }}
                            >
                              Download
                            </a>
                          ) : (
                            <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Archived on server</span>
                          )}
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

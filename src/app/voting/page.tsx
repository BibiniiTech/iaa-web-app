'use client';

import React, { useEffect, useState, useMemo } from 'react';
import { collection, doc, getDoc, getDocs, setDoc, query, where } from 'firebase/firestore';
import { auth, db } from '@/lib/firebase';
import { onAuthStateChanged } from 'firebase/auth';
import styles from './voting.module.css';

type VotingOption = {
  id: string;
  text: string;
  imageUrl: string;
};

type VotingCategory = {
  id: string;
  heading: string;
  options: VotingOption[];
};

type VotingConfig = {
  visible: boolean;
  header: string;
  categories: VotingCategory[];
};

export default function VotingPage() {
  const [config, setConfig] = useState<VotingConfig | null>(null);
  const [loading, setLoading] = useState(true);
  const [user, setUser] = useState<any>(null);
  const [hasVoted, setHasVoted] = useState(false);
  const [currentIdx, setCurrentIdx] = useState(0);
  const [selections, setSelections] = useState<Record<string, string>>({});
  const [allVotes, setAllVotes] = useState<any[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (u) => {
      setUser(u);
    });
    return () => unsubscribe();
  }, []);

  useEffect(() => {
    async function load() {
      if (!user) return;

      const [configSnap, voteSnap, allVotesSnap] = await Promise.all([
        getDoc(doc(db, 'config', 'voting_config')),
        getDoc(doc(db, 'votes', user.uid)),
        getDocs(collection(db, 'votes'))
      ]);

      if (configSnap.exists()) {
        setConfig(configSnap.data() as VotingConfig);
      }

      if (voteSnap.exists()) {
        setHasVoted(true);
      }

      setAllVotes(allVotesSnap.docs.map(d => d.data()));
      setLoading(false);
    }
    load();
  }, [user]);

  const stats = useMemo(() => {
    if (!config || !allVotes.length) return {};
    const currentCat = config.categories[currentIdx];
    if (!currentCat) return {};

    const counts: Record<string, number> = {};
    currentCat.options.forEach(opt => counts[opt.id] = 0);

    allVotes.forEach(vote => {
      const selection = vote.selections?.[currentCat.id];
      if (selection && counts[selection] !== undefined) {
        counts[selection]++;
      }
    });

    return counts;
  }, [config, allVotes, currentIdx]);

  if (loading) return <div className={styles.container}>Loading Voting...</div>;
  if (!config || !config.visible) return <div className={styles.container}>Voting is currently closed.</div>;
  if (hasVoted) return (
    <div className={styles.container}>
      <div className={styles.card}>
        <div className={styles.votedMessage}>
          <h2>Thank you for voting!</h2>
          <p>Your selections have been recorded.</p>
        </div>
      </div>
    </div>
  );

  const categories = config.categories;
  const isPreview = currentIdx === categories.length;
  const currentCategory = categories[currentIdx];

  async function handleSubmit() {
    if (!user) return;
    setIsSubmitting(true);
    try {
      await setDoc(doc(db, 'votes', user.uid), {
        userId: user.uid,
        selections,
        timestamp: Date.now()
      });
      setHasVoted(true);
    } catch (err) {
      alert('Error submitting vote. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  }

  const canGoNext = isPreview ? false : !!selections[currentCategory.id];

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <h1>{config.header}</h1>
        <p>Step {currentIdx + 1} of {categories.length + 1}</p>
      </div>

      {!isPreview ? (
        <>
          <div className={styles.card}>
            <h3>Live Stats</h3>
            <div className={styles.statsGrid}>
              {currentCategory.options.map(opt => (
                <div key={opt.id} className={styles.statItem}>
                  <span className={styles.statCount}>{stats[opt.id] || 0}</span>
                  <span className={styles.statLabel}>{opt.text}</span>
                </div>
              ))}
            </div>
          </div>

          <div className={styles.card}>
            <h2 className={styles.categoryHeading}>{currentCategory.heading}</h2>
            <div className={styles.optionsGrid}>
              {currentCategory.options.map(opt => (
                <div
                  key={opt.id}
                  className={`${styles.optionCard} ${selections[currentCategory.id] === opt.id ? styles.selectedOption : ''}`}
                  onClick={() => setSelections({...selections, [currentCategory.id]: opt.id})}
                >
                  {opt.imageUrl && <img src={opt.imageUrl} alt={opt.text} className={styles.optionImage} />}
                  <span className={styles.optionText}>{opt.text}</span>
                </div>
              ))}
            </div>
          </div>
        </>
      ) : (
        <div className={styles.card}>
          <h2 className={styles.categoryHeading}>Review Your Selections</h2>
          {categories.map(cat => (
            <div key={cat.id} className={styles.previewItem}>
              <span className={styles.previewCategory}>{cat.heading}</span>
              <span className={styles.previewSelection}>
                {cat.options.find(o => o.id === selections[cat.id])?.text || 'None'}
              </span>
            </div>
          ))}
          <button
            className={`${styles.navButton} ${styles.submitButton}`}
            onClick={handleSubmit}
            disabled={isSubmitting}
          >
            {isSubmitting ? 'Submitting...' : 'Confirm & Submit Vote'}
          </button>
        </div>
      )}

      <div className={styles.navigation}>
        <button
          className={`${styles.navButton} ${styles.prevButton}`}
          onClick={() => setCurrentIdx(currentIdx - 1)}
          disabled={currentIdx === 0}
        >
          Previous
        </button>
        {!isPreview && (
          <button
            className={`${styles.navButton} ${styles.nextButton}`}
            onClick={() => setCurrentIdx(currentIdx + 1)}
            disabled={!canGoNext}
          >
            Next
          </button>
        )}
      </div>
    </div>
  );
}

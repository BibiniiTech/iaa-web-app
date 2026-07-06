'use client';

import React, { useEffect, useState } from 'react';
import {
  collection,
  doc,
  getDoc,
  getDocs,
  query,
  where,
  addDoc,
  serverTimestamp,
  onSnapshot
} from 'firebase/firestore';
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
  name: string;
  options: VotingOption[];
  allowMultiple: boolean;
};

type VotingConfig = {
  visible: boolean;
  header: string;
  categories: VotingCategory[];
};

type VoteRecord = {
  userId: string;
  firstName: string;
  surname: string;
  email: string;
  phone: string;
  institutionType: string;
  institution: string;
  region: string;
  selections: Record<string, string[]>;
  timestamp: any;
};

type UserProfile = {
  uid: string;
  firstName: string;
  surname: string;
  email: string;
  institution: string;
  phone: string;
  role: string;
};

export default function VotingPage() {
  const [config, setConfig] = useState<VotingConfig | null>(null);
  const [userProfile, setUserProfile] = useState<UserProfile | null | undefined>(undefined);
  const [hasVoted, setHasVoted] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [allVotes, setAllVotes] = useState<VoteRecord[]>([]);

  const [currentStep, setCurrentStep] = useState(0);
  const [userSelections, setUserSelections] = useState<Record<string, string[]>>({}); // categoryId -> list of optionIds
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (user) => {
      if (user) {
        try {
          const profileSnap = await getDoc(doc(db, 'users', user.uid));
          if (profileSnap.exists()) {
            setUserProfile({ uid: user.uid, ...profileSnap.data() } as UserProfile);
          } else {
            console.error("User profile not found in Firestore for UID:", user.uid);
            setUserProfile(null); // Or handle as "not signed in/incomplete"
          }
        } catch (err) {
          console.error("Error fetching user profile:", err);
          setUserProfile(null);
        }
      } else {
        setUserProfile(null);
      }
    });
    return () => unsubscribe();
  }, []);

  useEffect(() => {
    let unsubscribeVotes: () => void = () => {};

    async function loadConfigAndVotes() {
      const currentUser = auth.currentUser;
      console.log("loadConfigAndVotes started. Profile UID:", userProfile?.uid, "Auth UID:", currentUser?.uid);

      try {
        // 1. Load Config
        console.log("Fetching config from 'config/voting_config'...");
        const configSnap = await getDoc(doc(db, 'config', 'voting_config'));
        if (configSnap.exists()) {
          const data = configSnap.data();
          setConfig({
            visible: data.visible ?? false,
            header: data.header || '',
            categories: data.categories || []
          } as VotingConfig);
          console.log("Config loaded successfully");
        } else {
          console.warn("Voting config document not found at 'config/voting_config'");
        }

        // 2. Setup real-time listener for votes
        console.log("Setting up votes collection listener...");
        unsubscribeVotes = onSnapshot(collection(db, 'votes'), (snapshot) => {
          console.log("Votes snapshot received. Size:", snapshot.size);
          const votesMap = new Map<string, VoteRecord>();

          snapshot.docs.forEach(d => {
            const data = d.data() as VoteRecord;
            // Use userId as unique key to prevent duplicates
            if (data.userId) {
              votesMap.set(data.userId, data);
            } else {
              // Fallback for any legacy data without userId
              votesMap.set(d.id, data);
            }
          });

          const votes = Array.from(votesMap.values());
          console.log("Unique votes processed:", votes.length);
          setAllVotes(votes);

          if (userProfile) {
            const hasVoted = votes.some(v => v.userId === userProfile.uid);
            setHasVoted(hasVoted);
          }
        }, (error) => {
          console.error("Error in onSnapshot listener for 'votes':", error.code, error.message);
        });

      } catch (error: any) {
        console.error("General error in loadConfigAndVotes:", error.code, error.message);
      } finally {
        setIsLoading(false);
      }
    }

    if (userProfile !== undefined) {
      if (userProfile) {
        loadConfigAndVotes();
      } else {
        setIsLoading(false);
      }
    }

    return () => unsubscribeVotes();
  }, [userProfile]);

  const handleSelectionChange = (categoryId: string, selectedOptions: string[]) => {
    setUserSelections(prev => ({
      ...prev,
      [categoryId]: selectedOptions
    }));
  };

  const handleSubmit = async () => {
    if (!userProfile || !config) return;

    // Check if all categories have at least one selection
    const allSelected = config.categories.every(cat =>
      userSelections[cat.id] && userSelections[cat.id].length > 0
    );

    if (!allSelected) {
      alert("Please make a selection for all categories.");
      return;
    }

    setIsSubmitting(true);
    try {
      const vote: VoteRecord = {
        userId: userProfile.uid,
        firstName: userProfile.firstName || '',
        surname: userProfile.surname || '',
        email: userProfile.email || '',
        phone: userProfile.phone || '',
        institutionType: userProfile.institution || '',
        institution: userProfile.institution || '',
        region: '', // Added as per Android model
        selections: userSelections,
        timestamp: serverTimestamp()
      };
      
      await addDoc(collection(db, 'votes'), vote);
      setHasVoted(true);
    } catch (error) {
      console.error("Error submitting vote:", error);
      alert("Failed to submit vote. Please try again.");
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <div className={styles.container}>
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '50vh' }}>
          <div className={styles.loadingSpinner}>Loading...</div>
        </div>
      </div>
    );
  }

  if (!config || !config.visible) {
    return (
      <div className={styles.container}>
        <div className={styles.card} style={{ textAlign: 'center' }}>
          <p>Voting is currently closed. Please check back later.</p>
        </div>
      </div>
    );
  }

  if (hasVoted) {
    return <VotingSuccessView config={config} allVotes={allVotes} />;
  }

  const categories = config.categories;
  const totalSteps = categories.length + 1; // +1 for summary card

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <h1>Voting Center</h1>
      </div>

      <div className={styles.card}>
        <h2 style={{ color: 'var(--primary)', textAlign: 'center', fontWeight: '800' }}>
          {config.header}
        </h2>

        <div style={{ margin: '20px 0' }}>
          <div style={{
            width: '100%',
            height: '8px',
            background: '#eee',
            borderRadius: '4px',
            overflow: 'hidden'
          }}>
            <div style={{
              width: `${((currentStep + 1) / totalSteps) * 100}%`,
              height: '100%',
              background: 'var(--primary)',
              transition: 'width 0.3s ease'
            }} />
          </div>
        </div>

        {currentStep < categories.length ? (
          <CategoryVotingCard
            category={categories[currentStep]}
            allVotes={allVotes}
            selectedOptions={userSelections[categories[currentStep].id] || []}
            onSelectionChange={(opts) => handleSelectionChange(categories[currentStep].id, opts)}
          />
        ) : (
          <VotingSummaryCard
            categories={categories}
            selections={userSelections}
            isSubmitting={isSubmitting}
            onSubmit={handleSubmit}
          />
        )}

        <div className={styles.navigation}>
          <button
            className={`${styles.navButton} ${styles.prevButton}`}
            onClick={() => setCurrentStep(prev => Math.max(0, prev - 1))}
            disabled={currentStep === 0}
          >
            ← Previous
          </button>

          {currentStep < totalSteps - 1 && (
            <button
              className={`${styles.navButton} ${styles.nextButton}`}
              onClick={() => setCurrentStep(prev => prev + 1)}
              disabled={!(userSelections[categories[currentStep].id]?.length > 0)}
            >
              Next →
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

function CategoryVotingCard({
  category,
  allVotes,
  selectedOptions,
  onSelectionChange
}: {
  category: VotingCategory;
  allVotes: VoteRecord[];
  selectedOptions: string[];
  onSelectionChange: (opts: string[]) => void;
}) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <ResultsSummaryCard category={category} allVotes={allVotes} />

      <h2 className={styles.categoryHeading} style={{ textAlign: 'center', marginBottom: '0' }}>
        {category.name}
      </h2>
      <p style={{ textAlign: 'center', color: '#666', fontSize: '0.85rem' }}>
        {category.allowMultiple ? "You can select multiple options" : "Select only one option"}
      </p>

      <div className={styles.optionsGrid}>
        {category.options.map(option => {
          const isSelected = selectedOptions.includes(option.id);
          return (
            <div
              key={option.id}
              className={`${styles.optionCard} ${isSelected ? styles.selectedOption : ''}`}
              onClick={() => {
                if (category.allowMultiple) {
                  if (isSelected) {
                    onSelectionChange(selectedOptions.filter(id => id !== option.id));
                  } else {
                    onSelectionChange([...selectedOptions, option.id]);
                  }
                } else {
                  onSelectionChange([option.id]);
                }
              }}
            >
              <img src={option.imageUrl} alt={option.text} className={styles.optionImage} />
              <div style={{ position: 'relative' }}>
                <span className={styles.optionText}>{option.text}</span>
                {isSelected && (
                  <div style={{
                    position: 'absolute',
                    top: '-10px',
                    right: '-5px',
                    color: 'var(--primary)',
                    fontSize: '1.2rem'
                  }}>✓</div>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function ResultsSummaryCard({
  category,
  allVotes
}: {
  category: VotingCategory;
  allVotes: VoteRecord[];
}) {
  // Stats Calculation
  const totalCategoryVotes = allVotes.filter(v =>
    v.selections &&
    v.selections[category.id] &&
    v.selections[category.id].length > 0
  ).length;

  const optionCounts = category.options.reduce((acc, option) => {
    acc[option.id] = allVotes.filter(v =>
      v.selections &&
      v.selections[category.id] &&
      v.selections[category.id].includes(option.id)
    ).length;
    return acc;
  }, {} as Record<string, number>);

  return (
    <div style={{
      background: 'rgba(27, 54, 93, 0.05)',
      padding: '20px',
      borderRadius: '15px',
      border: '1px solid rgba(27, 54, 93, 0.1)'
    }}>
      <h3 style={{ color: 'var(--primary)', fontSize: '0.9rem', marginBottom: '8px', textAlign: 'center' }}>
        Real-Time Results
      </h3>
      <p style={{ fontWeight: 'bold', textAlign: 'center', marginBottom: '16px' }}>
        {totalCategoryVotes} Total Votes cast for this category
      </p>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
        {category.options.map(option => {
          const count = optionCounts[option.id] || 0;
          const percent = totalCategoryVotes > 0 ? Math.round((count / totalCategoryVotes) * 100) : 0;
          return (
            <div key={option.id}>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', marginBottom: '4px' }}>
                <span>{option.text}</span>
                <strong>{count} ({percent}%)</strong>
              </div>
              <div style={{ width: '100%', height: '4px', background: '#ddd', borderRadius: '2px', overflow: 'hidden' }}>
                <div style={{ width: `${percent}%`, height: '100%', background: 'var(--primary)' }} />
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function VotingSummaryCard({
  categories,
  selections,
  isSubmitting,
  onSubmit
}: {
  categories: VotingCategory[];
  selections: Record<string, string[]>;
  isSubmitting: boolean;
  onSubmit: () => void;
}) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      <h2 className={styles.categoryHeading} style={{ textAlign: 'center' }}>Review Your Choices</h2>

      {categories.map(category => {
        const selectedIds = selections[category.id] || [];
        const selectedTexts = category.options
          .filter(opt => selectedIds.includes(opt.id))
          .map(opt => opt.text);

        return (
          <div key={category.id} className={styles.card} style={{ padding: '15px', margin: '0', background: '#fcfcfc' }}>
            <h4 style={{ color: 'var(--primary)', fontWeight: 'bold', marginBottom: '5px' }}>{category.name}</h4>
            {selectedTexts.length > 0 ? (
              <p>{selectedTexts.join(', ')}</p>
            ) : (
              <p style={{ color: 'red', fontSize: '0.85rem' }}>No selection</p>
            )}
          </div>
        );
      })}

      <div style={{ marginTop: '20px' }}>
        <button
          className={styles.submitButton}
          onClick={onSubmit}
          disabled={isSubmitting || !categories.every(cat => selections[cat.id]?.length > 0)}
        >
          {isSubmitting ? 'Submitting...' : 'Confirm & Submit Vote'}
        </button>
        <p style={{ textAlign: 'center', color: '#666', fontSize: '0.85rem', marginTop: '10px' }}>
          Note: Once submitted, you cannot change your vote.
        </p>
      </div>
    </div>
  );
}

function VotingSuccessView({
  config,
  allVotes
}: {
  config: VotingConfig | null;
  allVotes: VoteRecord[];
}) {
  return (
    <div className={styles.container}>
      <div className={styles.card} style={{ textAlign: 'center', padding: '40px 20px' }}>
        <div style={{ fontSize: '4rem', color: '#4CAF50', marginBottom: '10px' }}>✓</div>
        <h2 style={{ fontSize: '1.8rem', fontWeight: 'bold', marginBottom: '10px' }}>Thank You for Voting!</h2>
        <p style={{ color: '#666', marginBottom: '30px' }}>
          Your selections have been recorded. Here are the latest live results:
        </p>

        {config && config.categories.map(category => (
          <div key={category.id} style={{ marginBottom: '30px', textAlign: 'left' }}>
            <h4 style={{ color: 'var(--primary)', borderBottom: '1px solid #eee', paddingBottom: '5px', marginBottom: '15px' }}>
              {category.name}
            </h4>
            <ResultsSummaryCard category={category} allVotes={allVotes} />
          </div>
        ))}

        <button
          className={styles.submitButton}
          style={{ marginTop: '20px', background: '#666' }}
          onClick={() => window.location.href = '/'}
        >
          Return to Dashboard
        </button>
      </div>
    </div>
  );
}

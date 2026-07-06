'use client';

import React, { useEffect, useState } from 'react';
import { collection, doc, getDoc, getDocs, query, where, orderBy, runTransaction } from 'firebase/firestore';
import { auth, db } from '@/lib/firebase';
import { onAuthStateChanged } from 'firebase/auth';
import styles from './voting.module.css';

type PollOption = {
  id: string;
  text: string;
  imageUrl: string;
  votes: number;
};

type Poll = {
  id: string;
  question: string;
  isActive: boolean;
  timestamp: number;
  totalVotes: number;
  options: PollOption[];
};

type VotingConfig = {
  visible: boolean;
  header: string;
};

export default function VotingPage() {
  const [config, setConfig] = useState<VotingConfig | null>(null);
  const [polls, setPolls] = useState<Poll[]>([]);
  const [votedPollIds, setVotedPollIds] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(true);
  const [user, setUser] = useState<any>(null);
  const [selections, setSelections] = useState<Record<string, string>>({});
  const [submittingPollId, setSubmittingPollId] = useState<string | null>(null);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (u) => {
      setUser(u);
    });
    return () => unsubscribe();
  }, []);

  useEffect(() => {
    async function load() {
      if (!user) return;

      try {
        const configSnap = await getDoc(doc(db, 'config', 'voting_config'));
        if (configSnap.exists()) {
          setConfig(configSnap.data() as VotingConfig);
        }

        const pollsSnap = await getDocs(query(collection(db, 'polls'), where('isActive', '==', true), orderBy('timestamp', 'desc')));
        const activePolls = pollsSnap.docs.map(d => ({ ...d.data(), id: d.id } as Poll));
        setPolls(activePolls);

        const votesSnap = await getDocs(query(collection(db, 'votes'), where('userId', '==', user.uid)));
        const votedIds = new Set<string>();
        votesSnap.docs.forEach(d => {
           votedIds.add(d.data().pollId);
        });
        setVotedPollIds(votedIds);
      } catch (err) {
        console.error("Error loading voting data:", err);
      } finally {
        setLoading(false);
      }
    }
    load();
  }, [user]);

  async function handleVote(pollId: string) {
    if (!user) return;
    const optionId = selections[pollId];
    if (!optionId) {
      alert('Please select an option before voting.');
      return;
    }

    setSubmittingPollId(pollId);
    try {
      await runTransaction(db, async (transaction) => {
        const voteRef = doc(db, 'votes', `${user.uid}_${pollId}`);
        const pollRef = doc(db, 'polls', pollId);

        const voteDoc = await transaction.get(voteRef);
        if (voteDoc.exists()) {
          throw new Error('You have already voted in this poll.');
        }

        const pollDoc = await transaction.get(pollRef);
        if (!pollDoc.exists()) {
          throw new Error('Poll does not exist.');
        }

        const pollData = pollDoc.data() as Poll;
        const optionIndex = pollData.options.findIndex(o => o.id === optionId);
        if (optionIndex === -1) {
          throw new Error('Option not found.');
        }

        pollData.options[optionIndex].votes += 1;
        pollData.totalVotes += 1;

        transaction.set(voteRef, {
          userId: user.uid,
          pollId: pollId,
          optionId: optionId,
          timestamp: Date.now()
        });
        transaction.update(pollRef, {
          options: pollData.options,
          totalVotes: pollData.totalVotes
        });
      });

      setVotedPollIds(prev => new Set(prev).add(pollId));
      
      setPolls(prevPolls => prevPolls.map(p => {
        if (p.id === pollId) {
          const newOptions = [...p.options];
          const optIdx = newOptions.findIndex(o => o.id === optionId);
          if (optIdx !== -1) {
            newOptions[optIdx] = { ...newOptions[optIdx], votes: newOptions[optIdx].votes + 1 };
          }
          return { ...p, options: newOptions, totalVotes: p.totalVotes + 1 };
        }
        return p;
      }));
      
    } catch (err: any) {
      alert(err.message || 'Error submitting vote. Please try again.');
    } finally {
      setSubmittingPollId(null);
    }
  }

  if (loading) return <div className={styles.container}>Loading Voting...</div>;
  if (!config || !config.visible) return <div className={styles.container}>Voting is currently closed.</div>;

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <h1>{config.header}</h1>
      </div>

      {polls.length === 0 ? (
        <div className={styles.card}>
          <p>No active polls at the moment.</p>
        </div>
      ) : (
        polls.map(poll => {
          const hasVoted = votedPollIds.has(poll.id);
          const isSubmitting = submittingPollId === poll.id;
          
          return (
            <div key={poll.id} className={styles.card} style={{ marginBottom: '2rem' }}>
              <h2 className={styles.categoryHeading}>{poll.question}</h2>
              <div className={styles.optionsGrid}>
                {poll.options.map(opt => {
                  const isSelected = selections[poll.id] === opt.id;
                  const percentage = poll.totalVotes > 0 ? Math.round((opt.votes / poll.totalVotes) * 100) : 0;
                  return (
                    <div
                      key={opt.id}
                      className={`${styles.optionCard} ${isSelected && !hasVoted ? styles.selectedOption : ''}`}
                      onClick={() => !hasVoted && setSelections({ ...selections, [poll.id]: opt.id })}
                      style={{ cursor: hasVoted ? 'default' : 'pointer' }}
                    >
                      {opt.imageUrl && <img src={opt.imageUrl} alt={opt.text} className={styles.optionImage} />}
                      <span className={styles.optionText}>{opt.text}</span>
                      {hasVoted && (
                        <div style={{ marginTop: '10px', width: '100%' }}>
                          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', marginBottom: '5px' }}>
                            <span>{opt.votes} votes</span>
                            <span>{percentage}%</span>
                          </div>
                          <div style={{ width: '100%', height: '8px', background: '#e0e0e0', borderRadius: '4px', overflow: 'hidden' }}>
                            <div style={{ width: `${percentage}%`, height: '100%', background: '#0056b3' }} />
                          </div>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
              {!hasVoted && (
                <div style={{ marginTop: '1rem', textAlign: 'right' }}>
                  <button 
                    className={`${styles.navButton} ${styles.submitButton}`} 
                    onClick={() => handleVote(poll.id)}
                    disabled={isSubmitting || !selections[poll.id]}
                  >
                    {isSubmitting ? 'Submitting...' : 'Cast Vote'}
                  </button>
                </div>
              )}
              {hasVoted && (
                <div style={{ marginTop: '1rem', textAlign: 'center', color: '#28a745', fontWeight: 'bold' }}>
                  You have already voted in this poll.
                </div>
              )}
            </div>
          );
        })
      )}
    </div>
  );
}

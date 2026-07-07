'use client';

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { onAuthStateChanged } from 'firebase/auth';
import { doc, setDoc } from 'firebase/firestore';
import { auth, db } from '@/lib/firebase';
import styles from '../login/login.module.css';
import { REGIONS, MMDA_DATA } from '@/data/mmda_data';
import { MDA_DATA, SOE_DATA } from '@/data/mda_data';

const INSTITUTION_TYPES = ["MDA", "MMDA", "RCC", "SOE"];

export default function ProfileCompletionPage() {
  const [firstName, setFirstName] = useState('');
  const [surname, setSurname] = useState('');
  const [phone, setPhone] = useState('');
  const [institutionType, setInstitutionType] = useState('');
  const [institutionName, setInstitutionName] = useState('');
  const [region, setRegion] = useState('');
  const [mmda, setMmda] = useState('');

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [checkingAuth, setCheckingAuth] = useState(true);
  const router = useRouter();

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (user) => {
      if (!user) {
        router.push('/login');
      } else {
        if (user.displayName) {
          const parts = user.displayName.split(' ');
          setFirstName(parts[0] || '');
          setSurname(parts.slice(1).join(' ') || '');
        }
        setCheckingAuth(false);
      }
    });
    return () => unsubscribe();
  }, [router]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const user = auth.currentUser;
    if (!user) return;

    setError(null);
    setLoading(true);

    try {
      const finalInstitution = (institutionType === 'MDA' || institutionType === 'SOE')
        ? institutionName
        : mmda;

      await setDoc(doc(db, 'users', user.uid), {
        uid: user.uid,
        firstName: firstName.trim(),
        surname: surname.trim(),
        fullName: `${firstName.trim()} ${surname.trim()}`, // Added for Android compatibility
        email: user.email,
        phone: phone.trim(),
        institutionType,
        institutionName: (institutionType === 'MDA' || institutionType === 'SOE') ? institutionName.trim() : '',
        region: (institutionType === 'RCC' || institutionType === 'MMDA') ? region : '',
        institution: finalInstitution.trim(), // For Android app compatibility
        role: 'user', // Default role
        isProfileComplete: true,
        updatedAt: new Date().toISOString()
      });

      router.push('/');
    } catch (err: any) {
      setError(err.message || 'Failed to save profile');
    } finally {
      setLoading(false);
    }
  };

  if (checkingAuth) return <div style={{ padding: '2rem', textAlign: 'center' }}>Checking authentication...</div>;

  return (
    <div className={styles.authContainer} style={{ minHeight: '100vh', padding: '2rem 1rem' }}>
      <div className={styles.authCard} style={{ maxWidth: '600px' }}>
        <h1 className={styles.authTitle}>Complete Your Profile</h1>
        <p className={styles.authSubtitle}>We need a few more details to set up your account</p>

        {error && <div className={styles.errorMessage}>{error}</div>}

        <form className={styles.authForm} onSubmit={handleSubmit}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
            <div className={styles.inputGroup}>
              <label htmlFor="firstName">First Name</label>
              <input
                type="text"
                id="firstName"
                className={styles.inputField}
                value={firstName}
                onChange={(e) => setFirstName(e.target.value)}
                required
              />
            </div>
            <div className={styles.inputGroup}>
              <label htmlFor="surname">Surname</label>
              <input
                type="text"
                id="surname"
                className={styles.inputField}
                value={surname}
                onChange={(e) => setSurname(e.target.value)}
                required
              />
            </div>
          </div>

          <div className={styles.inputGroup}>
            <label htmlFor="phone">Phone Number</label>
            <input
              type="tel"
              id="phone"
              className={styles.inputField}
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              placeholder="e.g. 0244000000"
              required
            />
          </div>

          <div className={styles.inputGroup}>
            <label htmlFor="institutionType">Institution Type</label>
            <select
              id="institutionType"
              className={styles.inputField}
              value={institutionType}
              onChange={(e) => {
                setInstitutionType(e.target.value);
                setInstitutionName('');
                setRegion('');
                setMmda('');
              }}
              required
            >
              <option value="">Select Type</option>
              {INSTITUTION_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
            </select>
          </div>

          {(institutionType === 'MDA' || institutionType === 'SOE') && (
            <div className={styles.inputGroup}>
              <label htmlFor="institutionName">Institution Name ({institutionType})</label>
              <select
                id="institutionName"
                className={styles.inputField}
                value={institutionName}
                onChange={(e) => setInstitutionName(e.target.value)}
                required
              >
                <option value="">Select Institution</option>
                {(institutionType === 'MDA' ? MDA_DATA : SOE_DATA).map(inst => (
                  <option key={inst} value={inst}>{inst}</option>
                ))}
              </select>
            </div>
          )}

          {(institutionType === 'RCC' || institutionType === 'MMDA') && (
            <>
              <div className={styles.inputGroup}>
                <label htmlFor="region">Region</label>
                <select
                  id="region"
                  className={styles.inputField}
                  value={region}
                  onChange={(e) => {
                    setRegion(e.target.value);
                    setMmda('');
                  }}
                  required
                >
                  <option value="">Select Region</option>
                  {REGIONS.map(r => <option key={r} value={r}>{r}</option>)}
                </select>
              </div>

              {region && (
                <div className={styles.inputGroup}>
                  <label htmlFor="mmda">Specific Institution ({institutionType})</label>
                  <select
                    id="mmda"
                    className={styles.inputField}
                    value={mmda}
                    onChange={(e) => setMmda(e.target.value)}
                    required
                  >
                    <option value="">Select Institution</option>
                    {MMDA_DATA[region]
                      ?.filter(m => institutionType === 'MMDA' ? !m.endsWith(' RCC') : m.endsWith(' RCC'))
                      .map(m => <option key={m} value={m}>{m}</option>)
                    }
                  </select>
                </div>
              )}
            </>
          )}

          <button type="submit" className={styles.submitButton} disabled={loading}>
            {loading ? 'Saving Profile...' : 'Complete Registration'}
          </button>
        </form>
      </div>
    </div>
  );
}

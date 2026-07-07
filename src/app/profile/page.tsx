'use client';

import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { onAuthStateChanged, signOut, deleteUser } from 'firebase/auth';
import { doc, getDoc, updateDoc, deleteDoc } from 'firebase/firestore';
import { auth, db } from '@/lib/firebase';
import styles from './profile.module.css';
import { REGIONS, MMDA_DATA } from '@/data/mmda_data';
import { MDA_DATA, SOE_DATA } from '@/data/mda_data';

type UserProfile = {
  uid: string;
  email: string;
  firstName: string;
  surname: string;
  phone: string;
  institution: string;
  institutionType?: string;
  region?: string;
};

export default function ProfilePage() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [profile, setProfile] = useState<UserProfile | null>(null);

  // Form fields
  const [firstName, setFirstName] = useState('');
  const [surname, setSurname] = useState('');
  const [phone, setPhone] = useState('');
  const [institution, setInstitution] = useState('');
  const [institutionType, setInstitutionType] = useState('');
  const [region, setRegion] = useState('');

  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (user) => {
      if (!user) {
        router.push('/login');
        return;
      }

      try {
        const userDoc = await getDoc(doc(db, 'users', user.uid));
        if (userDoc.exists()) {
          const data = userDoc.data();
          const p: UserProfile = {
            uid: user.uid,
            email: user.email || '',
            firstName: data.firstName || '',
            surname: data.surname || '',
            phone: data.phone || data.phoneNumber || '',
            institution: data.institution || '',
            institutionType: data.institutionType || '',
            region: data.region || '',
          };
          setProfile(p);
          setFirstName(p.firstName);
          setSurname(p.surname);
          setPhone(p.phone);
          setInstitution(p.institution);
          setInstitutionType(p.institutionType || '');
          setRegion(p.region || '');
        }
      } catch (error) {
        console.error("Error fetching profile:", error);
      } finally {
        setLoading(false);
      }
    });

    return () => unsubscribe();
  }, [router]);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!profile) return;

    setIsSaving(true);
    setMessage(null);
    try {
      await updateDoc(doc(db, 'users', profile.uid), {
        firstName: firstName.trim(),
        surname: surname.trim(),
        phone: phone.trim(),
        institution: institution.trim(),
        institutionType: institutionType,
        region: region,
      });
      setMessage({ type: 'success', text: 'Profile updated successfully!' });
    } catch (error) {
      console.error("Error updating profile:", error);
      setMessage({ type: 'error', text: 'Failed to update profile.' });
    } finally {
      setIsSaving(false);
    }
  };

  if (loading) {
    return (
      <div className={styles.container}>
        <div className={styles.loading}>Loading Profile...</div>
      </div>
    );
  }

  return (
    <div className={styles.container}>
      <div className={styles.profileHeader}>
        <div className={styles.avatar}>
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
          </svg>
        </div>
        <h1 style={{ margin: 0 }}>My Profile</h1>
        <span className={styles.email}>{profile?.email}</span>
      </div>

      {message && (
        <div style={{
          padding: '1rem',
          borderRadius: '8px',
          marginBottom: '1rem',
          backgroundColor: message.type === 'success' ? 'rgba(76, 175, 80, 0.12)' : 'rgba(244, 67, 54, 0.12)',
          color: message.type === 'success' ? '#2e7d32' : '#d32f2f',
          fontWeight: 'bold',
          textAlign: 'center'
        }}>
          {message.text}
        </div>
      )}

      <form className={styles.form} onSubmit={handleSave}>
        <div className={styles.section}>
          <h2>Personal Information</h2>
          <div className={styles.form} style={{ gap: '1rem' }}>
            <div className={styles.formGroup}>
              <label>First Name</label>
              <div className={styles.inputWrapper}>
                <svg className={styles.inputIcon} viewBox="0 0 24 24" fill="currentColor">
                  <path d="M20 6h-8.18C11.4 4.84 10.3 4 9 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm0 12H4V6h5c.55 0 1 .45 1 1v1h10v10z" />
                </svg>
                <input
                  className={styles.input}
                  value={firstName}
                  onChange={(e) => setFirstName(e.target.value)}
                  placeholder="First Name"
                />
              </div>
            </div>

            <div className={styles.formGroup}>
              <label>Surname</label>
              <div className={styles.inputWrapper}>
                <svg className={styles.inputIcon} viewBox="0 0 24 24" fill="currentColor">
                  <path d="M20 6h-8.18C11.4 4.84 10.3 4 9 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm0 12H4V6h5c.55 0 1 .45 1 1v1h10v10z" />
                </svg>
                <input
                  className={styles.input}
                  value={surname}
                  onChange={(e) => setSurname(e.target.value)}
                  placeholder="Surname"
                />
              </div>
            </div>

            <div className={styles.formGroup}>
              <label>Phone Number</label>
              <div className={styles.inputWrapper}>
                <svg className={styles.inputIcon} viewBox="0 0 24 24" fill="currentColor">
                  <path d="M6.62 10.79c1.44 2.83 3.76 5.14 6.59 6.59l2.2-2.2c.27-.27.67-.36 1.02-.24 1.12.37 2.33.57 3.57.57.55 0 1 .45 1 1V20c0 .55-.45 1-1 1-9.39 0-17-7.61-17-17 0-.55.45-1 1-1h3.5c.55 0 1 .45 1 1 0 1.25.2 2.45.57 3.57.11.35.03.74-.25 1.02l-2.2 2.2z" />
                </svg>
                <input
                  className={styles.input}
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="Phone Number"
                  type="tel"
                />
              </div>
            </div>

            <div className={styles.formGroup}>
              <label>Institution Type</label>
              <div className={styles.inputWrapper}>
                <select
                  className={styles.input}
                  value={institutionType}
                  onChange={(e) => {
                    setInstitutionType(e.target.value);
                    setInstitution('');
                    setRegion('');
                  }}
                >
                  <option value="">Select Type</option>
                  <option value="MDA">MDA</option>
                  <option value="SOE">SOE</option>
                  <option value="RCC">RCC</option>
                  <option value="MMDA">MMDA</option>
                </select>
              </div>
            </div>

            {(institutionType === 'MDA' || institutionType === 'SOE') && (
              <div className={styles.formGroup}>
                <label>Institution Name</label>
                <div className={styles.inputWrapper}>
                  <select
                    className={styles.input}
                    value={institution}
                    onChange={(e) => setInstitution(e.target.value)}
                  >
                    <option value="">Select Institution</option>
                    {(institutionType === 'MDA' ? MDA_DATA : SOE_DATA).map(inst => (
                      <option key={inst} value={inst}>{inst}</option>
                    ))}
                  </select>
                </div>
              </div>
            )}

            {(institutionType === 'RCC' || institutionType === 'MMDA') && (
              <>
                <div className={styles.formGroup}>
                  <label>Region</label>
                  <div className={styles.inputWrapper}>
                    <select
                      className={styles.input}
                      value={region}
                      onChange={(e) => {
                        setRegion(e.target.value);
                        setInstitution('');
                      }}
                    >
                      <option value="">Select Region</option>
                      {REGIONS.map(r => <option key={r} value={r}>{r}</option>)}
                    </select>
                  </div>
                </div>

                {region && (
                  <div className={styles.formGroup}>
                    <label>Specific Institution</label>
                    <div className={styles.inputWrapper}>
                      <select
                        className={styles.input}
                        value={institution}
                        onChange={(e) => setInstitution(e.target.value)}
                      >
                        <option value="">Select Institution</option>
                        {MMDA_DATA[region]
                          ?.filter(m => institutionType === 'MMDA' ? !m.endsWith(' RCC') : m.endsWith(' RCC'))
                          .map(m => <option key={m} value={m}>{m}</option>)
                        }
                      </select>
                    </div>
                  </div>
                )}
              </>
            )}
          </div>
          <button className={styles.saveButton} type="submit" disabled={isSaving}>
            {isSaving ? 'Saving...' : 'Save Changes'}
          </button>
        </div>
      </form>

      <div className={styles.divider} />

      <ExpandableItem title="Privacy Policy" icon="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 10.5h7c-.51 4.12-3.1 7.75-7 8.92V11.5H5V6.3l7-3.11v8.31z">
        <p>We value your privacy. This app collects basic profile information (name, institution, contact) to facilitate audit report submissions to the Internal Audit Agency (IAA). Data is stored securely in Firebase and is only used for official IAA purposes. We do not share your information with third parties for marketing. <br /> sites.google.com/view/iaaappprivacypolicy</p>
      </ExpandableItem>

      <ExpandableItem title="Terms of Use" icon="M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zm-2 15H7v-1.5c0-1.66 3.33-2.5 5-2.5s5 .84 5 2.5V17h-5zm0-5c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm1-3V3.5L18.5 9H13z">
        <p>By using the IAA App, you agree to submit accurate and truthful information. Misuse of the reporting system may lead to administrative action. The resources provided are for guidance and should be used in conjunction with the relevant laws and regulations of Ghana.</p>
      </ExpandableItem>

      <ExpandableItem title="Account Management" icon="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 3c1.66 0 3 1.34 3 3s-1.34 3-3 3-3-1.34-3-3 1.34-3 3-3zm0 14.2c-2.5 0-4.71-1.28-6-3.22.03-1.99 4-3.08 6-3.08 1.99 0 5.97 1.09 6 3.08-1.29 1.94-3.5 3.22-6 3.22z">
        <AccountDeletion profile={profile} />
      </ExpandableItem>

      <ExpandableItem title="App Information" icon="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z">
        <div className={styles.infoList}>
          <p><strong>App Name:</strong> IAA App</p>
          <p><strong>Version:</strong> 1.8.0</p>
          <p><strong>Developer:</strong> Bibinii Tech <br /> for Internal Audit Agency</p>
        </div>
      </ExpandableItem>

      <div style={{ height: '4rem' }} />
    </div>
  );
}

function ExpandableItem({ title, icon, children }: { title: string; icon: string; children: React.ReactNode }) {
  const [expanded, setExpanded] = useState(false);

  return (
    <div className={`${styles.expandable} ${expanded ? styles.expanded : ''}`}>
      <button className={styles.expandableHeader} onClick={() => setExpanded(!expanded)} type="button">
        <svg className={styles.expandableIcon} viewBox="0 0 24 24" fill="currentColor">
          <path d={icon} />
        </svg>
        <span className={styles.expandableTitle}>{title}</span>
        <svg className={styles.expandIcon} viewBox="0 0 24 24" fill="currentColor">
          <path d="M7.41 8.59L12 13.17l4.59-4.58L18 10l-6 6-6-6 1.41-1.41z" />
        </svg>
      </button>
      {expanded && (
        <div className={styles.expandableContent}>
          {children}
        </div>
      )}
    </div>
  );
}

function AccountDeletion({ profile }: { profile: UserProfile | null }) {
  const [showConfirm, setShowConfirm] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const router = useRouter();

  const handleDelete = async () => {
    if (!auth.currentUser || !profile) return;
    setIsDeleting(true);
    try {
      await deleteDoc(doc(db, 'users', profile.uid));
      await deleteUser(auth.currentUser);
      router.push('/login');
    } catch (error) {
      console.error("Error deleting account:", error);
      alert("Failed to delete account. You may need to sign in again for security.");
      await signOut(auth);
      router.push('/login');
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <div className={styles.deleteSection}>
      <p className={styles.deleteNote}>
        To delete your account and all associated data, click the button below. This action is permanent and cannot be undone.
      </p>

      {!showConfirm ? (
        <button className={styles.deleteButton} onClick={() => setShowConfirm(true)} type="button">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
            <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z" />
          </svg>
          Request Account Deletion
        </button>
      ) : (
        <div className={styles.confirmDeleteRow}>
          <button className={styles.confirmDeleteBtn} onClick={handleDelete} disabled={isDeleting} type="button">
            {isDeleting ? 'Deleting...' : 'Confirm Delete'}
          </button>
          <button className={styles.cancelDeleteBtn} onClick={() => setShowConfirm(false)} type="button">
            Cancel
          </button>
        </div>
      )}
    </div>
  );
}

'use client';

import React, { useEffect, useState } from 'react';
import { doc, getDoc } from 'firebase/firestore';
import { db } from '@/lib/firebase';
import styles from './contact.module.css';
import Image from 'next/image';

interface ContactConfig {
  address: string;
  phone: string;
  emails: string;
  specificDesks: string;
  facebookUrl: string;
  instagramUrl: string;
  xUrl: string;
  locationQuery: string;
}

export default function ContactPage() {
  const [contact, setContact] = useState<ContactConfig>({
    address: 'Tumu Avenue, Kanda Accra - Ghana\nDigital Address: GV-002-6511',
    phone: '+233 (0) 362 196 941',
    emails: 'info@iaa.gov.gh\niaamails@iaa.gov.gh',
    specificDesks: 'CCCC: +233 (0) 362 196 941\nNACAP: +233 (0) 362 196 941\nTrainings: +233 (0) 362 196 941',
    facebookUrl: 'https://facebook.com',
    instagramUrl: 'https://instagram.com',
    xUrl: 'https://x.com',
    locationQuery: 'Internal Audit Agency, Accra'
  });

  useEffect(() => {
    async function fetchContact() {
      try {
        const docSnap = await getDoc(doc(db, 'config', 'contact'));
        if (docSnap.exists()) {
          setContact(docSnap.data() as ContactConfig);
        }
      } catch (error) {
        console.error("Error fetching contact config:", error);
      }
    }
    fetchContact();
  }, []);

  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <h1>Contact Us</h1>
      </header>

      <div className={styles.content}>
        <div className={styles.infoCard}>
          <div className={styles.icon}>📍</div>
          <h3>Head Office</h3>
          <p>{contact.address}</p>
        </div>

        <div className={styles.mapCard}>
          <div className={styles.mapPlaceholder}>
             <div className={styles.placeholderText}>
                Map Preview<br/>(Interactive Map not supported in preview)
             </div>
          </div>
          <div className={styles.mapFooter}>
            <div>
              <h4>Google Maps Location</h4>
              <p>Tap to open in full app</p>
            </div>
            <a
              href={`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(contact.locationQuery)}`}
              target="_blank"
              rel="noopener noreferrer"
              className={styles.mapButton}
            >
              🗺️
            </a>
          </div>
        </div>

        <div className={styles.infoCard} style={{ cursor: 'pointer' }} onClick={() => window.location.href = `tel:${contact.phone.replace(/[^0-9+]/g, '')}`}>
          <div className={styles.icon}>📞</div>
          <h3>Phone</h3>
          <p>{contact.phone}</p>
        </div>

        <div className={styles.infoCard} style={{ cursor: 'pointer' }} onClick={() => window.location.href = `mailto:${contact.emails.split(/\s+/)[0]}`}>
          <div className={styles.icon}>✉️</div>
          <h3>Email</h3>
          <p>{contact.emails}</p>
        </div>

        <div className={styles.infoCard}>
          <div className={styles.icon}>🏢</div>
          <h3>Specific Desks</h3>
          <p style={{ whiteSpace: 'pre-line' }}>{contact.specificDesks}</p>
        </div>

        <section className={styles.socialSection}>
          <h3>Follow Us</h3>
          <div className={styles.socialRow}>
            <a href={contact.facebookUrl} target="_blank" rel="noopener noreferrer" className={styles.socialIcon} style={{ backgroundColor: '#1877F2' }}>
              <Image src="/ic_facebook.png" alt="Facebook" width={28} height={28} />
            </a>
            <a href={contact.instagramUrl} target="_blank" rel="noopener noreferrer" className={styles.socialIcon} style={{ backgroundColor: '#E4405F' }}>
              <Image src="/ic_instagram.png" alt="Instagram" width={28} height={28} />
            </a>
            <a href={contact.xUrl} target="_blank" rel="noopener noreferrer" className={styles.socialIcon} style={{ backgroundColor: '#000000' }}>
              <Image src="/ic_x.png" alt="X" width={28} height={28} />
            </a>
          </div>
        </section>
      </div>
    </div>
  );
}

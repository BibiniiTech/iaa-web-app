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

const ContactIcons = {
  Location: () => (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
      <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
    </svg>
  ),
  Phone: () => (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
      <path d="M6.62 10.79c1.44 2.83 3.76 5.14 6.59 6.59l2.2-2.2c.27-.27.67-.36 1.02-.24 1.12.37 2.33.57 3.57.57.55 0 1 .45 1 1V20c0 .55-.45 1-1 1-9.39 0-17-7.61-17-17 0-.55.45-1 1-1h3.5c.55 0 1 .45 1 1 0 1.25.2 2.45.57 3.57.11.35.03.74-.25 1.02l-2.2 2.2z"/>
    </svg>
  ),
  Email: () => (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
      <path d="M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/>
    </svg>
  ),
  Business: () => (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
      <path d="M12 7V3H2v18h20V7H12zM6 19H4v-2h2v2zm0-4H4v-2h2v2zm0-4H4V9h2v2zm0-4H4V5h2v2zm4 12H8v-2h2v2zm0-4H8v-2h2v2zm0-4H8V9h2v2zm0-4H8V5h2v2zm10 12h-8v-2h2v-2h-2v-2h2v-2h-2V9h8v10zm-2-8h-2v2h2v-2zm0 4h-2v2h2v-2z"/>
    </svg>
  ),
  Map: () => (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
      <path d="M20.5 3l-.16.03L15 5.1 9 3 3.36 4.9c-.21.07-.36.25-.36.48V20.5c0 .28.22.5.5.5l.16-.03L9 18.9l6 2.1 5.64-1.9c.21-.07.36-.25.36-.48V3.5c0-.28-.22-.5-.5-.5zM15 19l-6-2.11V5l6 2.11V19z"/>
    </svg>
  )
};

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
          <div className={styles.icon}><ContactIcons.Location /></div>
          <h3>Head Office</h3>
          <p>{contact.address}</p>
        </div>

        <div className={styles.mapCard}>
          <div className={styles.mapContainer}>
             <iframe
                title="IAA Location"
                width="100%"
                height="100%"
                style={{ border: 0 }}
                loading="lazy"
                allowFullScreen
                src={`https://maps.google.com/maps?q=${encodeURIComponent(contact.locationQuery)}&t=&z=15&ie=UTF8&iwloc=&output=embed`}
             ></iframe>
          </div>
          <div className={styles.mapFooter}>
            <div>
              <h4>Google Maps Location</h4>
              <p>View larger map</p>
            </div>
            <a
              href={`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(contact.locationQuery)}`}
              target="_blank"
              rel="noopener noreferrer"
              className={styles.mapButton}
            >
              <ContactIcons.Map />
            </a>
          </div>
        </div>

        <div className={styles.infoCard} style={{ cursor: 'pointer' }} onClick={() => window.location.href = `tel:${contact.phone.replace(/[^0-9+]/g, '')}`}>
          <div className={styles.icon}><ContactIcons.Phone /></div>
          <h3>Phone</h3>
          <p>{contact.phone}</p>
        </div>

        <div className={styles.infoCard} style={{ cursor: 'pointer' }} onClick={() => window.location.href = `mailto:${contact.emails.split(/\s+/)[0]}`}>
          <div className={styles.icon}><ContactIcons.Email /></div>
          <h3>Email</h3>
          <p>{contact.emails}</p>
        </div>

        <div className={styles.infoCard}>
          <div className={styles.icon}><ContactIcons.Business /></div>
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

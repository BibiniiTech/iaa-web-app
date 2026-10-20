'use client';

import React from 'react';
import Image from 'next/image';
import styles from './disclaimer.module.css';

export default function DisclaimerPage() {
  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.logoContainer}>
          <Image
            src="/iaa_logo.png"
            alt="IAA Logo"
            width={240}
            height={100}
            className={styles.logo}
            style={{ width: 'auto', height: 'auto' }}
            priority
          />
        </div>
        <h1>Disclaimer</h1>
      </header>

      <div className={styles.content}>
        <div className={styles.card}>
          <div className={styles.textSection}>
            <h2 className={styles.cardTitle}>Terms of Use, Privacy &amp; Ownership</h2>

            <p className={styles.introText}>
              This App is NOT provided by the Internal Audit Agency (IAA) or its Affiliates.
              <br />
              It is a prototype to seek partnership for implementation and development from IAA and its Affiliates.
            </p>

            <section className={styles.disclaimerSection}>
              <h3 className={styles.sectionTitle}>Accuracy of Information</h3>
              <p>
                While the App was developed with real-life data to test functionality, accuracy can only be placed around 70%.
              </p>
            </section>

            <section className={styles.disclaimerSection}>
              <h3 className={styles.sectionTitle}>Data Protection</h3>
              <p>
                Submit Buttons have been disabled to prevent users from sharing data in this testing stage, and to prevent data leakage.
                <br />
                All information used in the App are already available and accessible by the general public and therefore not in breach of any data leakage policy.
                <br />
                Besides the App and its innovative features, we do not own the copyrights of the data used in this App
              </p>
            </section>
          </div>
        </div>

        <div className={styles.footer}>
          <p>
            &copy; 2026 Bibinii Tech. All rights reserved.
            <br />
            call/whatsapp: +233 544 737 870
            <br />
            email: bibiniitech@gmail.com
          </p>
        </div>
      </div>
    </div>
  );
}

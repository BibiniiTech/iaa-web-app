'use client';

import React from 'react';
import Image from 'next/image';
import styles from './Disclaimer.module.css';

interface DisclaimerProps {
  onAccept: () => void;
}

export default function Disclaimer({ onAccept }: DisclaimerProps) {
  return (
    <div className={styles.overlay}>
      <div className={styles.modal}>
        <div className={styles.header}>
          <Image src="/iaa_logo.png" alt="IAA Logo" width={180} height={60} priority />
        </div>
        <div className={styles.content}>
          <h2 className={styles.title}>Disclaimer</h2>
          <div className={styles.text}>
            <div className={styles.subtitle}>Terms of Use, Privacy &amp; Ownership</div>

            <p className={styles.intro}>
              This App is NOT provided by the Internal Audit Agency (IAA) or its Affiliates.
              <br />
              It is a prototype to seek partnership for implementation and development from IAA and its Affiliates.
            </p>

            <section className={styles.section}>
              <h3 className={styles.sectionTitle}>Accuracy of Information</h3>
              <p>
                While the App was developed with real-life data to test functionality, accuracy can only be placed around 70%.
              </p>
            </section>

            <section className={styles.section}>
              <h3 className={styles.sectionTitle}>Data Protection</h3>
              <p>
                Submit Buttons have been disabled to prevent users from sharing data in this testing stage, and to prevent data leakage.
                <br />
                All information used in the App are already available and accessible by the general public and therefore not in breach of any data leakage policy.
                <br />
                Besides the App and its innovative features, we do not own the copyrights of the data used in this App
              </p>
            </section>

            <div className={styles.footerNote}>
              &copy; 2026 Bibinii Tech. All rights reserved.
              <br />
              call/whatsapp: +233 544 737 870
              <br />
              email: bibiniitech@gmail.com
            </div>
          </div>
          <button className={styles.button} onClick={onAccept}>
            I AGREE
          </button>
        </div>
      </div>
    </div>
  );
}

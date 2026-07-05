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
          <h2 className={styles.title}>DISCLAIMER</h2>
          <div className={styles.text}>
            <section className={styles.section}>
              <h3 className={styles.sectionTitle}>WEBSITE DISCLAIMER</h3>
              <p>
                The information provided by the Internal Audit Agency ("we", "us", or "our") on this
                application is for general informational purposes only. All information on the
                application is provided in good faith, however we make no representation or warranty of
                any kind, express or implied, regarding the accuracy, adequacy, validity, reliability,
                availability, or completeness of any information on the application.
              </p>
              <p>
                Under no circumstance shall we have any liability to you for any loss or damage of any
                kind incurred as a result of the use of the application or reliance on any information
                provided on the application. Your use of the application and your reliance on any
                information on the application is solely at your own risk.
              </p>
            </section>

            <section className={styles.section}>
              <h3 className={styles.sectionTitle}>EXTERNAL LINKS DISCLAIMER</h3>
              <p>
                The application may contain links to other websites or content belonging to or
                originating from third parties. Such external links are not investigated, monitored, or
                checked for accuracy, adequacy, validity, reliability, availability, or completeness by
                us.
              </p>
            </section>

            <section className={styles.section}>
              <h3 className={styles.sectionTitle}>PROFESSIONAL DISCLAIMER</h3>
              <p>
                The application cannot and does not contain auditing/legal advice. The auditing/legal
                information is provided for general informational and educational purposes only and is
                not a substitute for professional advice.
              </p>
              <p>
                THE USE OR RELIANCE OF ANY INFORMATION CONTAINED ON THE APPLICATION IS SOLELY AT YOUR OWN
                RISK.
              </p>
            </section>
          </div>
          <button className={styles.button} onClick={onAccept}>
            I AGREE
          </button>
        </div>
      </div>
    </div>
  );
}

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
        <h1>DISCLAIMER</h1>
      </header>

      <div className={styles.content}>
        <div className={styles.card}>
          <div className={styles.iconContainer}>
            <svg width="48" height="48" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z" />
            </svg>
          </div>

          <div className={styles.textSection}>
            <section className={styles.disclaimerSection}>
              <h2 className={styles.sectionTitle}>WEBSITE DISCLAIMER</h2>
              <p>
                The information provided by the Internal Audit Agency ("we", "us", or "our") on this
                mobile application is for general informational purposes only. All information on the
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

            <section className={styles.disclaimerSection}>
              <h2 className={styles.sectionTitle}>EXTERNAL LINKS DISCLAIMER</h2>
              <p>
                The application may contain (or you may be sent through the application) links to other
                websites or content belonging to or originating from third parties or links to websites
                and features in banners or other advertising. Such external links are not investigated,
                monitored, or checked for accuracy, adequacy, validity, reliability, availability, or
                completeness by us.
              </p>
              <p>
                We do not warrant, endorse, guarantee, or assume responsibility for the accuracy or
                reliability of any information offered by third-party websites linked through the
                application or any website or feature linked in any banner or other advertising. We will
                not be a party to or in any way be responsible for monitoring any transaction between
                you and third-party providers of products or services.
              </p>
            </section>

            <section className={styles.disclaimerSection}>
              <h2 className={styles.sectionTitle}>PROFESSIONAL DISCLAIMER</h2>
              <p>
                The application cannot and does not contain auditing/legal advice. The auditing/legal
                information is provided for general informational and educational purposes only and is
                not a substitute for professional advice. Accordingly, before taking any actions based
                upon such information, we encourage you to consult with the appropriate professionals.
                We do not provide any kind of auditing/legal advice.
              </p>
              <p>
                THE USE OR RELIANCE OF ANY INFORMATION CONTAINED ON THE APPLICATION IS SOLELY AT YOUR OWN
                RISK.
              </p>
            </section>
          </div>
        </div>

        <div className={styles.footer}>
          <p>© {new Date().getFullYear()} Internal Audit Agency (IAA) Ghana. All rights reserved.</p>
        </div>
      </div>
    </div>
  );
}

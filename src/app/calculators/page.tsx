'use client';

import React, { useState } from 'react';
import PayrollCalculator from '@/components/calculators/PayrollCalculator';
import WithholdingCalculator from '@/components/calculators/WithholdingCalculator';
import styles from './page.module.css';

export default function CalculatorsPage() {
  const [activeTab, setActiveTab] = useState<'wht' | 'payroll'>('wht');

  return (
    <div className={styles.pageContainer}>
      <header className={styles.header}>
        <h1>Tax Calculators</h1>
        <p>Quick estimation tools for Withholding Taxes and GRA Payroll PAYE tax brackets.</p>
      </header>

      <div className={styles.tabContainer}>
        <div className={styles.tabs}>
          <button
            className={`${styles.tab} ${activeTab === 'wht' ? styles.activeTab : ''}`}
            onClick={() => setActiveTab('wht')}
          >
            Withholding Tax
          </button>
          <button
            className={`${styles.tab} ${activeTab === 'payroll' ? styles.activeTab : ''}`}
            onClick={() => setActiveTab('payroll')}
          >
            Payroll & PAYE
          </button>
        </div>

        <div className={styles.calculatorWrapper}>
          {activeTab === 'wht' ? <WithholdingCalculator /> : <PayrollCalculator />}
        </div>
      </div>
    </div>
  );
}

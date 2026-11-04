'use client';

import React, { useState } from 'react';
import PayrollCalculator from '@/components/calculators/PayrollCalculator';
import WithholdingCalculator from '@/components/calculators/WithholdingCalculator';
import VatCalculator from '@/components/calculators/VatCalculator';
import styles from './page.module.css';

export default function CalculatorsPage() {
  const [activeTab, setActiveTab] = useState<'wht' | 'payroll' | 'vat'>('wht');

  return (
    <div className={styles.pageContainer}>
      <header className={styles.header}>
        <h1>Tax Calculators</h1>
        <p>Quick estimation tools for Withholding Taxes, GRA Payroll PAYE tax brackets, and VAT & Statutory Levies.</p>
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
          <button
            className={`${styles.tab} ${activeTab === 'vat' ? styles.activeTab : ''}`}
            onClick={() => setActiveTab('vat')}
          >
            VAT & Levies
          </button>
        </div>

        <div className={styles.calculatorWrapper}>
          {activeTab === 'wht' && <WithholdingCalculator />}
          {activeTab === 'payroll' && <PayrollCalculator />}
          {activeTab === 'vat' && <VatCalculator />}
        </div>
      </div>
    </div>
  );
}

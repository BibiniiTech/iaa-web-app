'use client';

import React, { useState } from 'react';
import styles from './Calculators.module.css';

type VatScheme = 'standard' | 'flat';

export default function VatCalculator() {
  const [scheme, setScheme] = useState<VatScheme>('standard');
  const [baseInput, setBaseInput] = useState('');
  const [grossInput, setGrossInput] = useState('');
  const [isUpdatingFromBase, setIsUpdatingFromBase] = useState(true);

  const isStandard = scheme === 'standard';

  const baseVal = isUpdatingFromBase
    ? (parseFloat(baseInput) || 0)
    : isStandard
      ? (parseFloat(grossInput) || 0) / 1.219
      : (parseFloat(grossInput) || 0) / 1.04;

  const grossVal = isUpdatingFromBase
    ? isStandard
      ? (parseFloat(baseInput) || 0) * 1.219
      : (parseFloat(baseInput) || 0) * 1.04
    : (parseFloat(grossInput) || 0);

  // Statutory levies
  const nhilVal = isStandard ? baseVal * 0.025 : 0;
  const getfundVal = isStandard ? baseVal * 0.025 : 0;
  const covidVal = baseVal * 0.01;
  const totalLevies = nhilVal + getfundVal + covidVal;

  const vatBase = isStandard ? baseVal + totalLevies : baseVal;
  const vatVal = isStandard ? vatBase * 0.15 : baseVal * 0.03;
  const totalTaxAndLevies = totalLevies + vatVal;

  const handleSchemeChange = (newScheme: VatScheme) => {
    setScheme(newScheme);
    const standard = newScheme === 'standard';
    if (isUpdatingFromBase) {
      const b = parseFloat(baseInput) || 0;
      setGrossInput(b > 0 ? (standard ? (b * 1.219).toFixed(2) : (b * 1.04).toFixed(2)) : '');
    } else {
      const g = parseFloat(grossInput) || 0;
      setBaseInput(g > 0 ? (standard ? (g / 1.219).toFixed(2) : (g / 1.04).toFixed(2)) : '');
    }
  };

  const handleBaseChange = (val: string) => {
    setIsUpdatingFromBase(true);
    setBaseInput(val);
    const b = parseFloat(val) || 0;
    setGrossInput(b > 0 ? (isStandard ? (b * 1.219).toFixed(2) : (b * 1.04).toFixed(2)) : '');
  };

  const handleGrossChange = (val: string) => {
    setIsUpdatingFromBase(false);
    setGrossInput(val);
    const g = parseFloat(val) || 0;
    setBaseInput(g > 0 ? (isStandard ? (g / 1.219).toFixed(2) : (g / 1.04).toFixed(2)) : '');
  };

  const handleClear = () => {
    setBaseInput('');
    setGrossInput('');
    setIsUpdatingFromBase(true);
  };

  const formatCurrency = (val: number) =>
    new Intl.NumberFormat('en-GH', { style: 'currency', currency: 'GHS' }).format(val);

  return (
    <div className={styles.calculatorContainer}>
      <h2 className={styles.calcTitle}>VAT & Statutory Levies</h2>

      <div className={styles.inputArea}>
        <div className={styles.inputGroup}>
          <label>Tax Scheme</label>
          <select
            value={scheme}
            onChange={(e) => handleSchemeChange(e.target.value as VatScheme)}
          >
            <option value="standard">Standard Scheme (21.9% Effective Rate)</option>
            <option value="flat">Flat Rate Scheme - VFRS (4.0% Total Rate)</option>
          </select>
        </div>

        <div className={styles.inputGroup}>
          <label>Taxable Base Supply (Excl. VAT & Levies)</label>
          <input
            type="number"
            value={isUpdatingFromBase ? baseInput : baseVal > 0 ? baseVal.toFixed(2) : ''}
            onChange={(e) => handleBaseChange(e.target.value)}
            placeholder="0.00"
          />
        </div>

        <div className={styles.inputGroup}>
          <label>Gross Invoice Amount (Incl. VAT & Levies)</label>
          <input
            type="number"
            value={!isUpdatingFromBase ? grossInput : grossVal > 0 ? grossVal.toFixed(2) : ''}
            onChange={(e) => handleGrossChange(e.target.value)}
            placeholder="0.00"
          />
        </div>
      </div>

      <div className={styles.resultsCard}>
        <h3>Statutory Breakdown (GRA Standard)</h3>

        <div className={styles.resultRow}>
          <span>Taxable Base Supply:</span>
          <strong>{formatCurrency(baseVal)}</strong>
        </div>

        {isStandard && (
          <>
            <div className={styles.resultRow}>
              <span>NHIL (2.5%):</span>
              <span>{formatCurrency(nhilVal)}</span>
            </div>
            <div className={styles.resultRow}>
              <span>GETFund Levy (2.5%):</span>
              <span>{formatCurrency(getfundVal)}</span>
            </div>
          </>
        )}

        <div className={styles.resultRow}>
          <span>COVID-19 Health Recovery Levy (1.0%):</span>
          <span>{formatCurrency(covidVal)}</span>
        </div>

        <div className={styles.resultRow}>
          <span>{isStandard ? 'Total Levies (6.0%):' : 'Total Levies (1.0%):'}</span>
          <strong>{formatCurrency(totalLevies)}</strong>
        </div>

        <hr className={styles.divider} />

        <div className={styles.resultRow}>
          <span>{isStandard ? 'VAT (15% on Base + Levies):' : 'VAT Flat Rate (3.0%):'}</span>
          <span style={{ color: 'var(--primary)', fontWeight: 600 }}>{formatCurrency(vatVal)}</span>
        </div>

        <div className={styles.resultRow}>
          <span>Total Tax & Levies ({isStandard ? '21.9%' : '4.0%'}):</span>
          <strong className={styles.deduction}>{formatCurrency(totalTaxAndLevies)}</strong>
        </div>

        <div className={`${styles.resultRow} ${styles.netRow}`}>
          <span>Total Gross Invoice:</span>
          <strong className={styles.netValue}>{formatCurrency(grossVal)}</strong>
        </div>

        <p style={{
          fontSize: '0.8rem',
          color: 'var(--neutral-slate)',
          marginTop: '0.75rem',
          lineHeight: '1.4',
          borderTop: '1px dashed var(--border-color)',
          paddingTop: '0.75rem',
          textAlign: 'justify'
        }}>
          {isStandard
            ? 'Statutory basis: Ghana Value Added Tax Act, 2013 (Act 870) as amended. NHIL (2.5%), GETFund (2.5%), and COVID-19 (1.0%) levies form part of the taxable base for calculating 15% VAT, yielding an effective combined rate of 21.9%.'
            : 'Statutory basis: VAT Flat Rate Scheme (VFRS) under Act 870 as amended. Applied at 3% VAT + 1% COVID-19 Health Recovery Levy on taxable supplies for eligible retailers and wholesalers.'}
        </p>
      </div>

      <button className={styles.clearButton} onClick={handleClear}>
        Clear
      </button>
    </div>
  );
}

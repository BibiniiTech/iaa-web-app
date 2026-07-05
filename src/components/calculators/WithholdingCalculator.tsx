'use client';

import React, { useState, useEffect } from 'react';
import styles from './Calculators.module.css';

const TAX_RATES = {
  'Goods (3%)': 0.03,
  'Works (5%)': 0.05,
  'Services (7.5%)': 0.075,
};

export default function WithholdingCalculator() {
  const [category, setCategory] = useState<keyof typeof TAX_RATES>('Goods (3%)');
  const [grossInput, setGrossInput] = useState('');
  const [netInput, setNetInput] = useState('');
  const [isUpdatingFromGross, setIsUpdatingFromGross] = useState(true);

  const taxRate = TAX_RATES[category];

  const grossVal = isUpdatingFromGross
    ? (parseFloat(grossInput) || 0)
    : (parseFloat(netInput) || 0) / (1 - taxRate);

  const netVal = isUpdatingFromGross
    ? (parseFloat(grossInput) || 0) * (1 - taxRate)
    : (parseFloat(netInput) || 0);

  const taxVal = grossVal * taxRate;

  useEffect(() => {
    if (isUpdatingFromGross) {
      const g = parseFloat(grossInput) || 0;
      setNetInput(g > 0 ? (g * (1 - taxRate)).toFixed(2) : '');
    } else {
      const n = parseFloat(netInput) || 0;
      setGrossInput(n > 0 ? (n / (1 - taxRate)).toFixed(2) : '');
    }
  }, [category, taxRate]);

  const handleGrossChange = (val: string) => {
    setIsUpdatingFromGross(true);
    setGrossInput(val);
    const g = parseFloat(val) || 0;
    setNetInput(g > 0 ? (g * (1 - taxRate)).toFixed(2) : '');
  };

  const handleNetChange = (val: string) => {
    setIsUpdatingFromGross(false);
    setNetInput(val);
    const n = parseFloat(val) || 0;
    setGrossInput(n > 0 ? (n / (1 - taxRate)).toFixed(2) : '');
  };

  const formatCurrency = (val: number) =>
    new Intl.NumberFormat('en-GH', { style: 'currency', currency: 'GHS' }).format(val);

  return (
    <div className={styles.calculatorContainer}>
      <h2 className={styles.calcTitle}>Withholding Tax (WHT)</h2>

      <div className={styles.inputArea}>
        <div className={styles.inputGroup}>
          <label>Transaction Type</label>
          <select value={category} onChange={(e) => setCategory(e.target.value as any)}>
            {Object.keys(TAX_RATES).map(rate => (
              <option key={rate} value={rate}>{rate}</option>
            ))}
          </select>
        </div>

        <div className={styles.inputGroup}>
          <label>Gross Invoice Amount (GHS)</label>
          <input
            type="number"
            value={isUpdatingFromGross ? grossInput : (grossVal > 0 ? grossVal.toFixed(2) : '')}
            onChange={(e) => handleGrossChange(e.target.value)}
            placeholder="0.00"
          />
        </div>

        <div className={styles.inputGroup}>
          <label>Net Pay Amount (GHS)</label>
          <input
            type="number"
            value={!isUpdatingFromGross ? netInput : (netVal > 0 ? netVal.toFixed(2) : '')}
            onChange={(e) => handleNetChange(e.target.value)}
            placeholder="0.00"
          />
        </div>
      </div>

      <div className={styles.resultsCard}>
        <h3>Calculation Summary</h3>
        <div className={styles.resultRow}>
          <span>Gross Amount:</span>
          <strong>{formatCurrency(grossVal)}</strong>
        </div>
        <div className={styles.resultRow}>
          <span>WHT Deducted ({(taxRate * 100).toFixed(1)}%):</span>
          <strong className={styles.deduction}>{formatCurrency(taxVal)}</strong>
        </div>
        <div className={`${styles.resultRow} ${styles.netRow}`}>
          <span>Net Amount Paid:</span>
          <strong className={styles.netValue}>{formatCurrency(netVal)}</strong>
        </div>
      </div>

      <button className={styles.clearButton} onClick={() => { setGrossInput(''); setNetInput(''); setIsUpdatingFromGross(true); }}>
        Clear
      </button>
    </div>
  );
}

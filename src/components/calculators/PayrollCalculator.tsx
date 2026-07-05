'use client';

import React, { useState } from 'react';
import styles from './Calculators.module.css';

const calculateGhanaPaye = (taxableIncome: number): number => {
  let remaining = taxableIncome;
  let tax = 0;

  // Bracket 1: First 490 @ 0%
  const b1 = Math.min(remaining, 490.0);
  tax += b1 * 0.0;
  remaining -= b1;
  if (remaining <= 0) return tax;

  // Bracket 2: Next 110 @ 5%
  const b2 = Math.min(remaining, 110.0);
  tax += b2 * 0.05;
  remaining -= b2;
  if (remaining <= 0) return tax;

  // Bracket 3: Next 130 @ 10%
  const b3 = Math.min(remaining, 130.0);
  tax += b3 * 0.10;
  remaining -= b3;
  if (remaining <= 0) return tax;

  // Bracket 4: Next 3,170 @ 17.5%
  const b4 = Math.min(remaining, 3170.0);
  tax += b4 * 0.175;
  remaining -= b4;
  if (remaining <= 0) return tax;

  // Bracket 5: Next 11,100 @ 25%
  const b5 = Math.min(remaining, 11100.0);
  tax += b5 * 0.25;
  remaining -= b5;
  if (remaining <= 0) return tax;

  // Bracket 6: Next 20,000 @ 30%
  const b6 = Math.min(remaining, 20000.0);
  tax += b6 * 0.30;
  remaining -= b6;
  if (remaining <= 0) return tax;

  // Bracket 7: Exceeding 35,000 @ 35%
  tax += remaining * 0.35;

  return tax;
};

export default function PayrollCalculator() {
  const [basicSalaryInput, setBasicSalaryInput] = useState('');
  const [allowancesInput, setAllowancesInput] = useState('');

  const basicSalary = parseFloat(basicSalaryInput) || 0;
  const allowances = parseFloat(allowancesInput) || 0;

  const grossSalary = basicSalary + allowances;
  const employeeSsnit = basicSalary * 0.055;
  const employerSsnit = basicSalary * 0.130;

  const taxableIncome = Math.max(0, grossSalary - employeeSsnit);
  const paye = calculateGhanaPaye(taxableIncome);

  const totalDeductions = employeeSsnit + paye;
  const netSalary = Math.max(0, grossSalary - totalDeductions);
  const totalCostOfLabor = grossSalary + employerSsnit;

  const formatCurrency = (val: number) =>
    new Intl.NumberFormat('en-GH', { style: 'currency', currency: 'GHS' }).format(val);

  return (
    <div className={styles.calculatorContainer}>
      <h2 className={styles.calcTitle}>Monthly Payroll & PAYE</h2>

      <div className={styles.inputArea}>
        <div className={styles.inputGroup}>
          <label>Basic Salary (GHS)</label>
          <input
            type="number"
            value={basicSalaryInput}
            onChange={(e) => setBasicSalaryInput(e.target.value)}
            placeholder="0.00"
          />
        </div>
        <div className={styles.inputGroup}>
          <label>Allowances (GHS)</label>
          <input
            type="number"
            value={allowancesInput}
            onChange={(e) => setAllowancesInput(e.target.value)}
            placeholder="0.00"
          />
        </div>
      </div>

      <div className={styles.resultsCard}>
        <h3>Deductions & Cost Breakdown</h3>
        <div className={styles.resultRow}>
          <span>Gross Salary:</span>
          <strong>{formatCurrency(grossSalary)}</strong>
        </div>
        <div className={styles.resultRow}>
          <span>Employee SSNIT (5.5%):</span>
          <strong className={styles.deduction}>{formatCurrency(employeeSsnit)}</strong>
        </div>
        <div className={styles.resultRow}>
          <span>Taxable Income:</span>
          <strong>{formatCurrency(taxableIncome)}</strong>
        </div>
        <div className={styles.resultRow}>
          <span>PAYE Tax (GRA):</span>
          <strong className={styles.deduction}>{formatCurrency(paye)}</strong>
        </div>
        <div className={`${styles.resultRow} ${styles.netRow}`}>
          <span>Net Take-Home:</span>
          <strong className={styles.netValue}>{formatCurrency(netSalary)}</strong>
        </div>
        <hr className={styles.divider} />
        <div className={styles.resultRow}>
          <span>Employer SSNIT (13%):</span>
          <strong>{formatCurrency(employerSsnit)}</strong>
        </div>
        <div className={styles.resultRow}>
          <span>Total Cost of Labor:</span>
          <strong>{formatCurrency(totalCostOfLabor)}</strong>
        </div>
      </div>

      <button className={styles.clearButton} onClick={() => { setBasicSalaryInput(''); setAllowancesInput(''); }}>
        Clear
      </button>
    </div>
  );
}

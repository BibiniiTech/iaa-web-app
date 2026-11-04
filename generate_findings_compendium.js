/*
 * ============================================================================
 * PROPRIETARY AND CONFIDENTIAL PROPERTY OF CLEMENT KOFI ACHEAMPONG
 * (BIBINII TECH) - ALL RIGHTS RESERVED.
 *
 * Application: Internal Auditing Support & Statutory Reporting Platform (IAA App)
 * Author: Clement Kofi Acheampong (Internal Auditor & Software Engineer)
 * Copyright (c) 2024-2026 Clement Kofi Acheampong. All Rights Reserved.
 *
 * NOTICE: All information, source code, algorithms, and data designs contained
 * herein are protected under the Ghana Copyright Act, 2005 (Act 690) and
 * international intellectual property treaties.
 * ============================================================================
 */

const fs = require('fs');
const path = require('path');

const docxPath = path.join(process.env.TEMP, 'docx_gen', 'node_modules', 'docx');
const {
  Document,
  Packer,
  Paragraph,
  TextRun,
  Table,
  TableRow,
  TableCell,
  WidthType,
  AlignmentType,
  PageOrientation,
  Header,
  Footer,
  PageNumber,
  BorderStyle,
  HeadingLevel,
  PageBreak
} = require(docxPath);

// 1. Load Data
const rawDataPath = fs.existsSync(path.join(__dirname, 'curated_data_backup', 'audit_findings.json'))
  ? path.join(__dirname, 'curated_data_backup', 'audit_findings.json')
  : path.join(__dirname, 'app', 'src', 'main', 'assets', 'audit_findings.json');
const findingsRaw = JSON.parse(fs.readFileSync(rawDataPath, 'utf8'));

console.log(`Loaded ${findingsRaw.length} raw findings.`);

// Category metadata
const CATEGORY_META = [
  {
    key: 'EXPENDITURE',
    title: 'Category 1: EXPENDITURE',
    scope: 'Covers unvouched/unsupported payments, GIFMIS bypass, unretired imprests, questioned expenditures, unauthorized cash disbursements, misapplication of funds, irregular allowances, and lack of internal voucher verification.',
    primaryActs: 'PFM Act 2016 (Act 921) §§ 7, 25, 30, 52, 96; PFM Regulations 2019 (L.I. 2378) Regs 78, 79, 81-83, 102, 232, 234, 235'
  },
  {
    key: 'REVENUE',
    title: 'Category 2: REVENUE',
    scope: 'Addresses unaccounted revenues, delayed bank lodgements, suppression of revenue receipts/value books, uncollected property rates and ground rents, non-billing of leases, missing bank reconciliations, and unauthorized revenue retentions.',
    primaryActs: 'PFM Act 2016 (Act 921) §§ 7, 10, 46, 96; PFM Regulations 2019 (L.I. 2378) Regs 33, 46, 47, 48, 50, 51; Local Governance Act 2016 (Act 936)'
  },
  {
    key: 'PROCUREMENT/CONTRACT/PROJECT',
    title: 'Category 3: PROCUREMENT / CONTRACT / PROJECT',
    scope: 'Covers procurement without PPA approval, contract splitting, single-source irregularities, advance mobilizations without bank guarantees, abandoned/delayed projects, defective execution, and absence of conflict-of-interest declarations.',
    primaryActs: 'Public Procurement Act 2003 (Act 663 as amended by Act 914) §§ 21, 38, 40, 44, 47, 86; PFM Act 2016 (Act 921)'
  },
  {
    key: 'STORES /LIBRARY',
    title: 'Category 4: STORES / LIBRARY',
    scope: 'Encompasses stores items not taken on ledger, unrecorded store receipts, items issued without Store Issue Vouchers (SIV) or signed distribution sheets, obsolete stock without Board of Survey, and missing inventory.',
    primaryActs: 'Stores Regulations 1984; PFM Act 2016 (Act 921) § 52; PFM Regulations 2019 (L.I. 2378) Regs 106-114'
  },
  {
    key: 'TRANSPORT',
    title: 'Category 5: TRANSPORT',
    scope: 'Details unauthorized fuel and lubricant purchases, unmaintained vehicle logbooks, excessive fuel consumption, unlicensed/uninsured official vehicles, unauthorized use of state vehicles, and unlogged transport repairs.',
    primaryActs: 'PFM Act 2016 (Act 921) § 52; PFM Regulations 2019 (L.I. 2378); National Transport Policy Guidelines'
  },
  {
    key: 'IT',
    title: 'Category 6: INFORMATION TECHNOLOGY (IT)',
    scope: 'Focuses on the lack of IT disaster recovery and business continuity plans, unbacked-up financial databases, unmanaged vendor SLA contracts, shared passwords, unauthorized database access, and unlicensed enterprise software.',
    primaryActs: 'Cybersecurity Act 2020 (Act 1038); Data Protection Act 2012 (Act 843); PFM Act 2016 (Act 921) § 52'
  },
  {
    key: 'ASSET',
    title: 'Category 7: ASSETS MANAGEMENT',
    scope: 'Covers missing physical assets, absence of comprehensive Fixed Asset Registers (FAR), unnumbered assets, failure to insure state properties, unauthorized disposal or cannibalization, and unboarded unserviceable equipment.',
    primaryActs: 'PFM Act 2016 (Act 921) §§ 52, 53; PFM Regulations 2019 (L.I. 2378) Regs 106, 150-160; Public Procurement Act 2003 (Act 663 / Act 914) Part VII'
  },
  {
    key: 'HUMAN RESOURCE / PAYROLL',
    title: 'Category 8: HUMAN RESOURCE / PAYROLL',
    scope: 'Addresses unearned salaries and pensions paid to deceased/separated staff, delayed deletion from ESPV payroll, unbonded study leaves, recruitments without Ministry of Finance clearance, and unconducted annual appraisals.',
    primaryActs: 'PFM Act 2016 (Act 921) § 7, § 96; PFM Regulations 2019 (L.I. 2378) Regs 86, 88, 92; Labour Act 2003 (Act 651)'
  },
  {
    key: 'TAX & STATUTORY DEDUCTIONS',
    title: 'Category 9: TAX & STATUTORY DEDUCTIONS',
    scope: 'Focuses on failure to withhold tax (WHT) from suppliers, non-remittance of deducted taxes to GRA, delayed SSNIT contributions attracting statutory interest penalties, and failure to remit Tier-2 occupational pension deductions.',
    primaryActs: 'Income Tax Act 2015 (Act 896) §§ 114, 116; Value Added Tax Act 2013 (Act 870); National Pensions Act 2008 (Act 766); Revenue Administration Act 2016 (Act 915)'
  },
  {
    key: 'FINANCIAL REPORTING',
    title: 'Category 10: FINANCIAL REPORTING',
    scope: 'Covers non-preparation and delayed submission of monthly/quarterly/annual financial statements, unreconciled trial balances, failure to publish audited accounts, and delayed management letter responses to audit queries.',
    primaryActs: 'PFM Act 2016 (Act 921) §§ 80, 81, 82, 96; PFM Regulations 2019 (L.I. 2378) Regs 190, 207; Audit Service Act 2000 (Act 584)'
  },
  {
    key: 'ADMINISTRATIVE / GOVERNANCE / OTHERS',
    title: 'Category 11: ADMINISTRATIVE / GOVERNANCE / OTHERS',
    scope: 'Details non-constitution or inactivity of statutory Audit Committees, non-implementation of internal and external audit recommendations, absence of District Assembly by-laws, and operating without enterprise risk management (ERM) frameworks.',
    primaryActs: 'Internal Audit Agency Act 2003 (Act 658) §§ 16, 20; PFM Act 2016 (Act 921) §§ 86, 88, 96; Local Governance Act 2016 (Act 936)'
  }
];

// Pre-process findings: hierarchical display numbers and propagate covered entities
const processedFindings = [];
let currentCategory = '';
let currentNum = '';
let subIndex = 0;
let currentApplicable = '';

findingsRaw.forEach((f) => {
  if (f.category !== currentCategory) {
    currentCategory = f.category;
    currentNum = '';
    subIndex = 0;
    currentApplicable = '';
  }

  // Numbering
  let displayNumber = '';
  if (f.number.startsWith('bullet_')) {
    subIndex++;
    displayNumber = currentNum ? `${currentNum}.${subIndex}` : `Sub-${subIndex}`;
  } else {
    currentNum = f.number;
    subIndex = 0;
    displayNumber = currentNum;
  }

  // Covered entity propagation
  if (f.applicableTo && f.applicableTo.trim().length > 0) {
    currentApplicable = f.applicableTo.replace(/\s+/g, ' ').trim();
  }
  const applicableToClean = currentApplicable || 'All covered entities';

  // Criteria cleaning
  const cleanedCriteria = (f.criteria || []).map(c => c.replace(/\s+/g, ' ').trim()).filter(c => c.length > 0);

  // Clean observation text
  const cleanedObservation = f.observation.replace(/\s+/g, ' ').trim();

  processedFindings.push({
    id: f.id,
    category: f.category,
    rawNumber: f.number,
    displayNumber,
    isBullet: f.number.startsWith('bullet_'),
    observation: cleanedObservation,
    criteria: cleanedCriteria,
    applicableTo: applicableToClean
  });
});

console.log(`Preprocessed ${processedFindings.length} findings.`);

// Styling helpers
const COLOR_PRIMARY = '0F2C59'; // Deep Navy
const COLOR_SECONDARY = '1B365D'; // Royal Navy
const COLOR_ACCENT = '2563EB'; // Vibrant Blue
const COLOR_MUTED = '64748B'; // Slate Gray
const COLOR_TEXT = '1E293B'; // Charcoal Dark
const COLOR_BG_LIGHT = 'F8FAFC'; // Clean Off-white/slate
const COLOR_BORDER = 'CBD5E1'; // Light Slate border
const COLOR_WHITE = 'FFFFFF';

const cellBorderStandard = {
  top: { style: BorderStyle.SINGLE, size: 1, color: COLOR_BORDER },
  bottom: { style: BorderStyle.SINGLE, size: 1, color: COLOR_BORDER },
  left: { style: BorderStyle.SINGLE, size: 1, color: COLOR_BORDER },
  right: { style: BorderStyle.SINGLE, size: 1, color: COLOR_BORDER }
};

const cellMarginsStandard = {
  top: 100,
  bottom: 100,
  left: 130,
  right: 130
};

// Criterion splitter for bolding section/regulation numbers
function splitCriterion(text) {
  const match = text.match(/^(Regulation\s+[^,]+?|Section\s+[^,]+?|Paragraph\s+[^,]+?|Article\s+[^,]+?)( of the .*| of .*|,.*|$)/i);
  if (match) {
    return { boldPart: match[1], rest: match[2] };
  }
  return { boldPart: '', rest: text };
}

// Build Front Matter Children
const frontMatterChildren = [];

// Header Title Block
frontMatterChildren.push(
  new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: { before: 180, after: 60 },
    children: [
      new TextRun({
        text: 'REPUBLIC OF GHANA • INTERNAL AUDIT AGENCY',
        bold: true,
        size: 20,
        color: COLOR_ACCENT,
        font: 'Calibri'
      })
    ]
  }),
  new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: { before: 40, after: 120 },
    children: [
      new TextRun({
        text: 'SAMPLE OF AUDIT OBSERVATIONS AND APPLICABLE LAWS / CRITERIA',
        bold: true,
        size: 36,
        color: COLOR_PRIMARY,
        font: 'Calibri'
      })
    ]
  }),
  new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: { before: 0, after: 240 },
    children: [
      new TextRun({
        text: '3RD EDITION COMPENDIUM • COMPREHENSIVE REPOSITORY OF AUDIT FINDINGS BY CATEGORY',
        bold: true,
        size: 22,
        color: COLOR_MUTED,
        font: 'Calibri'
      })
    ]
  })
);

// Metadata Summary Table (4 Columns)
const metaTable = new Table({
  width: { size: 14112, type: WidthType.DXA },
  rows: [
    new TableRow({
      cantSplit: true,
      children: [
        new TableCell({
          width: { size: 3528, type: WidthType.DXA },
          shading: { fill: 'EFF6FF' },
          margins: cellMarginsStandard,
          borders: cellBorderStandard,
          children: [
            new Paragraph({
              spacing: { before: 40, after: 20 },
              children: [new TextRun({ text: 'PUBLICATION & EDITION', bold: true, size: 17, color: COLOR_PRIMARY, font: 'Calibri' })]
            }),
            new Paragraph({
              spacing: { before: 0, after: 40 },
              children: [new TextRun({ text: 'Internal Audit Aid Compendium\n3rd Edition (June 2024)', size: 18, color: COLOR_TEXT, font: 'Calibri' })]
            })
          ]
        }),
        new TableCell({
          width: { size: 3528, type: WidthType.DXA },
          shading: { fill: 'EFF6FF' },
          margins: cellMarginsStandard,
          borders: cellBorderStandard,
          children: [
            new Paragraph({
              spacing: { before: 40, after: 20 },
              children: [new TextRun({ text: 'EDITORIAL & COMPILERS', bold: true, size: 17, color: COLOR_PRIMARY, font: 'Calibri' })]
            }),
            new Paragraph({
              spacing: { before: 0, after: 40 },
              children: [new TextRun({ text: 'Samuel Nyarko\nJohn Omiano Arthur\nRachael Dede Anim', size: 18, color: COLOR_TEXT, font: 'Calibri' })]
            })
          ]
        }),
        new TableCell({
          width: { size: 3528, type: WidthType.DXA },
          shading: { fill: 'EFF6FF' },
          margins: cellMarginsStandard,
          borders: cellBorderStandard,
          children: [
            new Paragraph({
              spacing: { before: 40, after: 20 },
              children: [new TextRun({ text: 'TOTAL AUDIT SCOPE', bold: true, size: 17, color: COLOR_PRIMARY, font: 'Calibri' })]
            }),
            new Paragraph({
              spacing: { before: 0, after: 40 },
              children: [new TextRun({ text: '634 Specific Audit Findings\n11 Standard Functional Categories', size: 18, color: COLOR_TEXT, font: 'Calibri' })]
            })
          ]
        }),
        new TableCell({
          width: { size: 3528, type: WidthType.DXA },
          shading: { fill: 'EFF6FF' },
          margins: cellMarginsStandard,
          borders: cellBorderStandard,
          children: [
            new Paragraph({
              spacing: { before: 40, after: 20 },
              children: [new TextRun({ text: 'TARGET AUDIENCE & COVERAGE', bold: true, size: 17, color: COLOR_PRIMARY, font: 'Calibri' })]
            }),
            new Paragraph({
              spacing: { before: 0, after: 40 },
              children: [new TextRun({ text: 'All Covered Entities (MDAs, MMDAs, Statutory & Constitutional Bodies)', size: 18, color: COLOR_TEXT, font: 'Calibri' })]
            })
          ]
        })
      ]
    })
  ]
});
frontMatterChildren.push(metaTable);

// Executive Overview Paragraph
frontMatterChildren.push(
  new Paragraph({
    spacing: { before: 200, after: 100 },
    children: [
      new TextRun({
        text: 'Executive Overview and Purpose',
        bold: true,
        size: 24,
        color: COLOR_PRIMARY,
        font: 'Calibri'
      })
    ]
  }),
  new Paragraph({
    spacing: { before: 40, after: 140 },
    children: [
      new TextRun({
        text: 'This reference compendium provides an exhaustive, authoritative catalogue of standard internal audit observations, procedural non-compliances, and associated statutory criteria governing the public financial administration of Ghana. Prepared under the auspices of Team Internal Audit Aid and aligned with the national mandate of the Internal Audit Agency (IAA), this document is structured across eleven (11) distinct functional domains to equip internal auditors, audit committees, accounting officers, and public sector oversight bodies with instant, legally grounded audit criteria.',
        size: 20,
        color: COLOR_TEXT,
        font: 'Calibri'
      })
    ]
  }),
  new Paragraph({
    spacing: { before: 40, after: 140 },
    children: [
      new TextRun({
        text: 'Every individual audit observation in this document is linked to specific sanction-backed provisions of the Public Financial Management Act, 2016 (Act 921), the Public Financial Management Regulations, 2019 (L.I. 2378), the Public Procurement Act, 2003 (Act 663 as amended by Act 914), the Local Governance Act, 2016 (Act 936), Stores Regulations 1984, the Labour Act, 2003 (Act 651), and related national tax and administrative directives. It establishes clear legal attribution and identifies the exact classes of covered entities bound by each statutory mandate.',
        size: 20,
        color: COLOR_TEXT,
        font: 'Calibri'
      })
    ]
  })
);

// Master Summary Table by Category
frontMatterChildren.push(
  new Paragraph({
    spacing: { before: 180, after: 80 },
    children: [
      new TextRun({
        text: 'Master Category Distribution & Statutory Reference Summary',
        bold: true,
        size: 22,
        color: COLOR_PRIMARY,
        font: 'Calibri'
      })
    ]
  })
);

const summaryTableRows = [
  new TableRow({
    tableHeader: true,
    cantSplit: true,
    children: [
      new TableCell({
        width: { size: 800, type: WidthType.DXA },
        shading: { fill: COLOR_PRIMARY },
        margins: cellMarginsStandard,
        borders: cellBorderStandard,
        children: [new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: 'No.', bold: true, color: COLOR_WHITE, size: 18, font: 'Calibri' })] })]
      }),
      new TableCell({
        width: { size: 3400, type: WidthType.DXA },
        shading: { fill: COLOR_PRIMARY },
        margins: cellMarginsStandard,
        borders: cellBorderStandard,
        children: [new Paragraph({ children: [new TextRun({ text: 'Functional Category', bold: true, color: COLOR_WHITE, size: 18, font: 'Calibri' })] })]
      }),
      new TableCell({
        width: { size: 4812, type: WidthType.DXA },
        shading: { fill: COLOR_PRIMARY },
        margins: cellMarginsStandard,
        borders: cellBorderStandard,
        children: [new Paragraph({ children: [new TextRun({ text: 'Core Audit Scope & Operational Focus', bold: true, color: COLOR_WHITE, size: 18, font: 'Calibri' })] })]
      }),
      new TableCell({
        width: { size: 1300, type: WidthType.DXA },
        shading: { fill: COLOR_PRIMARY },
        margins: cellMarginsStandard,
        borders: cellBorderStandard,
        children: [new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: 'Findings', bold: true, color: COLOR_WHITE, size: 18, font: 'Calibri' })] })]
      }),
      new TableCell({
        width: { size: 1100, type: WidthType.DXA },
        shading: { fill: COLOR_PRIMARY },
        margins: cellMarginsStandard,
        borders: cellBorderStandard,
        children: [new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: 'Share (%)', bold: true, color: COLOR_WHITE, size: 18, font: 'Calibri' })] })]
      }),
      new TableCell({
        width: { size: 2700, type: WidthType.DXA },
        shading: { fill: COLOR_PRIMARY },
        margins: cellMarginsStandard,
        borders: cellBorderStandard,
        children: [new Paragraph({ children: [new TextRun({ text: 'Primary Statutory Instruments', bold: true, color: COLOR_WHITE, size: 18, font: 'Calibri' })] })]
      })
    ]
  })
];

let totalCount = 0;
CATEGORY_META.forEach((cat, idx) => {
  const catFindings = processedFindings.filter(f => f.category === cat.key);
  const count = catFindings.length;
  totalCount += count;
  const share = ((count / processedFindings.length) * 100).toFixed(1);
  const rowBg = idx % 2 === 0 ? COLOR_WHITE : COLOR_BG_LIGHT;

  summaryTableRows.push(
    new TableRow({
      cantSplit: true,
      children: [
        new TableCell({
          width: { size: 800, type: WidthType.DXA },
          shading: { fill: rowBg },
          margins: cellMarginsStandard,
          borders: cellBorderStandard,
          children: [new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: `${idx + 1}`, bold: true, size: 18, color: COLOR_TEXT, font: 'Calibri' })] })]
        }),
        new TableCell({
          width: { size: 3400, type: WidthType.DXA },
          shading: { fill: rowBg },
          margins: cellMarginsStandard,
          borders: cellBorderStandard,
          children: [new Paragraph({ children: [new TextRun({ text: cat.key, bold: true, size: 18, color: COLOR_PRIMARY, font: 'Calibri' })] })]
        }),
        new TableCell({
          width: { size: 4812, type: WidthType.DXA },
          shading: { fill: rowBg },
          margins: cellMarginsStandard,
          borders: cellBorderStandard,
          children: [new Paragraph({ children: [new TextRun({ text: cat.scope, size: 17, color: COLOR_TEXT, font: 'Calibri' })] })]
        }),
        new TableCell({
          width: { size: 1300, type: WidthType.DXA },
          shading: { fill: rowBg },
          margins: cellMarginsStandard,
          borders: cellBorderStandard,
          children: [new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: `${count}`, bold: true, size: 18, color: COLOR_TEXT, font: 'Calibri' })] })]
        }),
        new TableCell({
          width: { size: 1100, type: WidthType.DXA },
          shading: { fill: rowBg },
          margins: cellMarginsStandard,
          borders: cellBorderStandard,
          children: [new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: `${share}%`, size: 18, color: COLOR_MUTED, font: 'Calibri' })] })]
        }),
        new TableCell({
          width: { size: 2700, type: WidthType.DXA },
          shading: { fill: rowBg },
          margins: cellMarginsStandard,
          borders: cellBorderStandard,
          children: [new Paragraph({ children: [new TextRun({ text: cat.primaryActs, size: 16, color: COLOR_MUTED, font: 'Calibri' })] })]
        })
      ]
    })
  );
});

// Summary Total Row
summaryTableRows.push(
  new TableRow({
    cantSplit: true,
    children: [
      new TableCell({
        width: { size: 800, type: WidthType.DXA },
        shading: { fill: 'E2E8F0' },
        margins: cellMarginsStandard,
        borders: cellBorderStandard,
        children: [new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: '—', bold: true, size: 18, font: 'Calibri' })] })]
      }),
      new TableCell({
        width: { size: 3400, type: WidthType.DXA },
        shading: { fill: 'E2E8F0' },
        margins: cellMarginsStandard,
        borders: cellBorderStandard,
        children: [new Paragraph({ children: [new TextRun({ text: 'TOTAL AUDIT REPOSITORY', bold: true, size: 18, color: COLOR_PRIMARY, font: 'Calibri' })] })]
      }),
      new TableCell({
        width: { size: 4812, type: WidthType.DXA },
        shading: { fill: 'E2E8F0' },
        margins: cellMarginsStandard,
        borders: cellBorderStandard,
        children: [new Paragraph({ children: [new TextRun({ text: 'Comprehensive national public sector internal audit criteria coverage across all operating units', bold: true, size: 17, color: COLOR_TEXT, font: 'Calibri' })] })]
      }),
      new TableCell({
        width: { size: 1300, type: WidthType.DXA },
        shading: { fill: 'E2E8F0' },
        margins: cellMarginsStandard,
        borders: cellBorderStandard,
        children: [new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: `${totalCount}`, bold: true, size: 19, color: COLOR_PRIMARY, font: 'Calibri' })] })]
      }),
      new TableCell({
        width: { size: 1100, type: WidthType.DXA },
        shading: { fill: 'E2E8F0' },
        margins: cellMarginsStandard,
        borders: cellBorderStandard,
        children: [new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: '100.0%', bold: true, size: 18, color: COLOR_PRIMARY, font: 'Calibri' })] })]
      }),
      new TableCell({
        width: { size: 2700, type: WidthType.DXA },
        shading: { fill: 'E2E8F0' },
        margins: cellMarginsStandard,
        borders: cellBorderStandard,
        children: [new Paragraph({ children: [new TextRun({ text: 'Statutory National Harmonization', bold: true, size: 16, color: COLOR_PRIMARY, font: 'Calibri' })] })]
      })
    ]
  })
);

frontMatterChildren.push(new Table({ width: { size: 14112, type: WidthType.DXA }, rows: summaryTableRows }));

// Primary Statutory Framework Table
frontMatterChildren.push(
  new Paragraph({
    spacing: { before: 200, after: 80 },
    children: [
      new TextRun({
        text: 'Governing Statutory & Regulatory Framework',
        bold: true,
        size: 22,
        color: COLOR_PRIMARY,
        font: 'Calibri'
      })
    ]
  })
);

const statutoryTableRows = [
  new TableRow({
    tableHeader: true,
    cantSplit: true,
    children: [
      new TableCell({
        width: { size: 4500, type: WidthType.DXA },
        shading: { fill: COLOR_SECONDARY },
        margins: cellMarginsStandard,
        borders: cellBorderStandard,
        children: [new Paragraph({ children: [new TextRun({ text: 'Legislation / Regulatory Instrument', bold: true, color: COLOR_WHITE, size: 18, font: 'Calibri' })] })]
      }),
      new TableCell({
        width: { size: 2000, type: WidthType.DXA },
        shading: { fill: COLOR_SECONDARY },
        margins: cellMarginsStandard,
        borders: cellBorderStandard,
        children: [new Paragraph({ children: [new TextRun({ text: 'Citation Ref', bold: true, color: COLOR_WHITE, size: 18, font: 'Calibri' })] })]
      }),
      new TableCell({
        width: { size: 6112, type: WidthType.DXA },
        shading: { fill: COLOR_SECONDARY },
        margins: cellMarginsStandard,
        borders: cellBorderStandard,
        children: [new Paragraph({ children: [new TextRun({ text: 'Substantive Regulatory Scope & Sanctions Powers', bold: true, color: COLOR_WHITE, size: 18, font: 'Calibri' })] })]
      }),
      new TableCell({
        width: { size: 1500, type: WidthType.DXA },
        shading: { fill: COLOR_SECONDARY },
        margins: cellMarginsStandard,
        borders: cellBorderStandard,
        children: [new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: 'Citations', bold: true, color: COLOR_WHITE, size: 18, font: 'Calibri' })] })]
      })
    ]
  }),
  {
    name: 'Public Financial Management Act, 2016',
    ref: 'Act 921',
    scope: 'Primary statute governing public financial management across all covered entities; prescribes fiscal discipline, budget execution, internal audit duties, and punitive offenses under Section 96.',
    citations: '789'
  },
  {
    name: 'Public Financial Management Regulations, 2019',
    ref: 'L.I. 2378',
    scope: 'Subordinate legislation detailing operational procedures for expenditure, commitments, GIFMIS usage, value books, revenue management, accountable imprests, and asset control.',
    citations: '406'
  },
  {
    name: 'Public Procurement Act, 2003 (as amended by Act 914)',
    ref: 'Act 663 / 914',
    scope: 'Governs public procurement processes, thresholds, tendering methods, single-source approvals, contract management, board of surveys, and disposal of public stores.',
    citations: '82'
  },
  {
    name: 'Local Governance Act, 2016',
    ref: 'Act 936',
    scope: 'Regulates Metropolitan, Municipal, and District Assemblies (MMDAs), local revenue generation, by-laws, financial administration, and District Internal Audit Units.',
    citations: '43'
  },
  {
    name: 'Stores Regulations, 1984',
    ref: 'Stores Regs 1984',
    scope: 'Prescribes mandatory custody, inventory accounting, store ledgers, tally cards, issuance procedures, and Board of Survey boarding for public stores.',
    citations: '27'
  },
  {
    name: 'Labour Act, 2003',
    ref: 'Act 651',
    scope: 'Governs employer-employee rights, leave policies, remuneration, termination procedures, study leaves, and statutory staff management standards.',
    citations: '11'
  },
  {
    name: 'Income Tax Act, 2015',
    ref: 'Act 896',
    scope: 'Mandates withholding tax (WHT) deductions at source, tax rates on goods and services, filing deadlines, and statutory interest for non-compliance.',
    citations: '6'
  },
  {
    name: 'Constitution of the Republic of Ghana, 1992',
    ref: '1992 Constitution',
    scope: 'Supreme law of Ghana; establishes public financial accountability, Auditor-General powers, consolidated fund rules, and parliamentary oversight.',
    citations: '4'
  },
  {
    name: 'Internal Audit Agency Act, 2003 & Audit Service Act, 2000',
    ref: 'Act 658 & Act 584',
    scope: 'Mandates internal audit structures, reporting standards, Audit Committee roles, response to management letters, and sanctions for financial indiscipline.',
    citations: '6'
  }
].map((item, idx) => {
  if (idx === 0) return item; // Header already pushed
  const rowBg = idx % 2 === 0 ? COLOR_WHITE : COLOR_BG_LIGHT;
  return new TableRow({
    cantSplit: true,
    children: [
      new TableCell({
        width: { size: 4500, type: WidthType.DXA },
        shading: { fill: rowBg },
        margins: cellMarginsStandard,
        borders: cellBorderStandard,
        children: [new Paragraph({ children: [new TextRun({ text: item.name, bold: true, size: 18, color: COLOR_TEXT, font: 'Calibri' })] })]
      }),
      new TableCell({
        width: { size: 2000, type: WidthType.DXA },
        shading: { fill: rowBg },
        margins: cellMarginsStandard,
        borders: cellBorderStandard,
        children: [new Paragraph({ children: [new TextRun({ text: item.ref, bold: true, size: 17, color: COLOR_PRIMARY, font: 'Calibri' })] })]
      }),
      new TableCell({
        width: { size: 6112, type: WidthType.DXA },
        shading: { fill: rowBg },
        margins: cellMarginsStandard,
        borders: cellBorderStandard,
        children: [new Paragraph({ children: [new TextRun({ text: item.scope, size: 17, color: COLOR_TEXT, font: 'Calibri' })] })]
      }),
      new TableCell({
        width: { size: 1500, type: WidthType.DXA },
        shading: { fill: rowBg },
        margins: cellMarginsStandard,
        borders: cellBorderStandard,
        children: [new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: item.citations, bold: true, size: 18, color: COLOR_PRIMARY, font: 'Calibri' })] })]
      })
    ]
  });
});

frontMatterChildren.push(new Table({ width: { size: 14112, type: WidthType.DXA }, rows: statutoryTableRows }));

// Page Break before detailed findings
frontMatterChildren.push(new Paragraph({ children: [new PageBreak()] }));

// Build Detailed Findings by Category
const allDocumentChildren = [...frontMatterChildren];

CATEGORY_META.forEach((catMeta, catIndex) => {
  const catFindings = processedFindings.filter(f => f.category === catMeta.key);

  // Section Heading
  allDocumentChildren.push(
    new Paragraph({
      spacing: { before: 200, after: 60 },
      children: [
        new TextRun({
          text: `${catMeta.title.toUpperCase()} (${catFindings.length} OBSERVATIONS / FINDINGS)`,
          bold: true,
          size: 26,
          color: COLOR_PRIMARY,
          font: 'Calibri'
        })
      ]
    }),
    new Paragraph({
      spacing: { before: 0, after: 120 },
      children: [
        new TextRun({
          text: `Audit Focus & Scope: `,
          bold: true,
          size: 18,
          color: COLOR_SECONDARY,
          font: 'Calibri'
        }),
        new TextRun({
          text: catMeta.scope,
          size: 18,
          color: COLOR_TEXT,
          font: 'Calibri'
        })
      ]
    })
  );

  // Table for this category
  // Widths: 1100 (Ref No), 5500 (Observation), 5012 (Applicable Criteria/Laws), 2500 (Covered Entity) = 14112 twips
  const tableRows = [
    new TableRow({
      tableHeader: true,
      cantSplit: true,
      children: [
        new TableCell({
          width: { size: 1100, type: WidthType.DXA },
          shading: { fill: COLOR_PRIMARY },
          margins: cellMarginsStandard,
          borders: cellBorderStandard,
          children: [
            new Paragraph({
              alignment: AlignmentType.CENTER,
              children: [new TextRun({ text: 'Ref No.', bold: true, color: COLOR_WHITE, size: 18, font: 'Calibri' })]
            })
          ]
        }),
        new TableCell({
          width: { size: 5500, type: WidthType.DXA },
          shading: { fill: COLOR_PRIMARY },
          margins: cellMarginsStandard,
          borders: cellBorderStandard,
          children: [
            new Paragraph({
              children: [new TextRun({ text: 'Audit Observation / Finding Description', bold: true, color: COLOR_WHITE, size: 18, font: 'Calibri' })]
            })
          ]
        }),
        new TableCell({
          width: { size: 5012, type: WidthType.DXA },
          shading: { fill: COLOR_PRIMARY },
          margins: cellMarginsStandard,
          borders: cellBorderStandard,
          children: [
            new Paragraph({
              children: [new TextRun({ text: 'Applicable Criteria / Laws & Regulations', bold: true, color: COLOR_WHITE, size: 18, font: 'Calibri' })]
            })
          ]
        }),
        new TableCell({
          width: { size: 2500, type: WidthType.DXA },
          shading: { fill: COLOR_PRIMARY },
          margins: cellMarginsStandard,
          borders: cellBorderStandard,
          children: [
            new Paragraph({
              children: [new TextRun({ text: 'Applicable Covered Entity', bold: true, color: COLOR_WHITE, size: 18, font: 'Calibri' })]
            })
          ]
        })
      ]
    })
  ];

  catFindings.forEach((finding, fIndex) => {
    const rowBg = fIndex % 2 === 0 ? COLOR_WHITE : COLOR_BG_LIGHT;

    // Ref No cell paragraphs
    const refParas = [
      new Paragraph({
        alignment: AlignmentType.CENTER,
        spacing: { before: 20, after: 20 },
        children: [
          new TextRun({
            text: finding.displayNumber,
            bold: true,
            size: finding.isBullet ? 17 : 19,
            color: finding.isBullet ? COLOR_ACCENT : COLOR_PRIMARY,
            font: 'Calibri'
          })
        ]
      })
    ];

    // Observation cell paragraphs
    const obsParas = [
      new Paragraph({
        spacing: { before: 20, after: 20 },
        children: [
          new TextRun({
            text: finding.observation,
            bold: !finding.isBullet,
            size: 18,
            color: COLOR_TEXT,
            font: 'Calibri'
          })
        ]
      })
    ];

    // Criteria cell paragraphs
    const critParas = [];
    if (finding.criteria.length === 0) {
      critParas.push(
        new Paragraph({
          spacing: { before: 20, after: 20 },
          children: [
            new TextRun({
              text: 'Standard Public Sector Financial & Administrative Regulations',
              italics: true,
              size: 17,
              color: COLOR_MUTED,
              font: 'Calibri'
            })
          ]
        })
      );
    } else {
      finding.criteria.forEach((critText, cIdx) => {
        const { boldPart, rest } = splitCriterion(critText);
        const children = [
          new TextRun({
            text: '• ',
            bold: true,
            size: 17,
            color: COLOR_ACCENT,
            font: 'Calibri'
          })
        ];
        if (boldPart) {
          children.push(
            new TextRun({
              text: boldPart,
              bold: true,
              size: 17,
              color: COLOR_PRIMARY,
              font: 'Calibri'
            })
          );
        }
        children.push(
          new TextRun({
            text: rest,
            size: 17,
            color: COLOR_TEXT,
            font: 'Calibri'
          })
        );

        critParas.push(
          new Paragraph({
            spacing: { before: cIdx === 0 ? 20 : 30, after: 20 },
            children
          })
        );
      });
    }

    // Applicable entity cell paragraphs
    const appParas = [
      new Paragraph({
        spacing: { before: 20, after: 20 },
        children: [
          new TextRun({
            text: finding.applicableTo,
            size: 17,
            color: COLOR_TEXT,
            font: 'Calibri'
          })
        ]
      })
    ];

    tableRows.push(
      new TableRow({
        cantSplit: true,
        children: [
          new TableCell({
            width: { size: 1100, type: WidthType.DXA },
            shading: { fill: rowBg },
            margins: cellMarginsStandard,
            borders: cellBorderStandard,
            children: refParas
          }),
          new TableCell({
            width: { size: 5500, type: WidthType.DXA },
            shading: { fill: rowBg },
            margins: cellMarginsStandard,
            borders: cellBorderStandard,
            children: obsParas
          }),
          new TableCell({
            width: { size: 5012, type: WidthType.DXA },
            shading: { fill: rowBg },
            margins: cellMarginsStandard,
            borders: cellBorderStandard,
            children: critParas
          }),
          new TableCell({
            width: { size: 2500, type: WidthType.DXA },
            shading: { fill: rowBg },
            margins: cellMarginsStandard,
            borders: cellBorderStandard,
            children: appParas
          })
        ]
      })
    );
  });

  allDocumentChildren.push(
    new Table({
      width: { size: 14112, type: WidthType.DXA },
      rows: tableRows
    })
  );

  // Add PageBreak after each category table (except the last category)
  if (catIndex < CATEGORY_META.length - 1) {
    allDocumentChildren.push(new Paragraph({ children: [new PageBreak()] }));
  }
});

// Create Document
const doc = new Document({
  styles: {
    default: {
      document: {
        run: {
          font: 'Calibri',
          color: COLOR_TEXT
        }
      }
    }
  },
  sections: [
    {
      properties: {
        page: {
          size: {
            orientation: PageOrientation.LANDSCAPE,
            width: 15840,
            height: 12240
          },
          margin: {
            top: 864,
            bottom: 864,
            left: 864,
            right: 864
          }
        }
      },
      headers: {
        default: new Header({
          children: [
            new Paragraph({
              alignment: AlignmentType.RIGHT,
              spacing: { after: 80 },
              children: [
                new TextRun({
                  text: 'Team Internal Audit Aid • Sample of Audit Observations & Applicable Laws/Criteria (3rd Edition)',
                  size: 16,
                  color: COLOR_MUTED,
                  font: 'Calibri',
                  italics: true
                })
              ]
            })
          ]
        })
      },
      footers: {
        default: new Footer({
          children: [
            new Paragraph({
              alignment: AlignmentType.RIGHT,
              spacing: { before: 80 },
              children: [
                new TextRun({
                  text: 'Internal Audit Agency (IAA) App Knowledge Base  |  Page ',
                  size: 16,
                  color: COLOR_MUTED,
                  font: 'Calibri'
                }),
                PageNumber.CURRENT,
                new TextRun({
                  text: ' of ',
                  size: 16,
                  color: COLOR_MUTED,
                  font: 'Calibri'
                }),
                PageNumber.TOTAL_PAGES
              ]
            })
          ]
        })
      },
      children: allDocumentChildren
    }
  ]
});

// Generate and save
console.log('Packaging document with docx Packer...');
Packer.toBuffer(doc).then((buffer) => {
  const outputPrimary = path.join(__dirname, 'IAA_Audit_Findings_and_Categories_Compendium.docx');
  const outputSecondary = path.join(__dirname, 'Audit_Findings_and_Categories.docx');

  fs.writeFileSync(outputPrimary, buffer);
  fs.writeFileSync(outputSecondary, buffer);

  console.log(`Successfully generated Word documents:`);
  console.log(`1. ${outputPrimary} (${buffer.length} bytes)`);
  console.log(`2. ${outputSecondary} (${buffer.length} bytes)`);
}).catch((err) => {
  console.error('Error generating document:', err);
  process.exit(1);
});

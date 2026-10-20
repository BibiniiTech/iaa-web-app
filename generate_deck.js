const {
  Document,
  Packer,
  Paragraph,
  TextRun,
  HeadingLevel,
  AlignmentType,
  Table,
  TableRow,
  TableCell,
  WidthType,
  BorderStyle,
  ShadingType,
  PageBreak,
  Header,
  Footer,
  PageNumber,
} = require('docx');
const fs = require('fs');
const path = require('path');

function createCell(text, isHeader = false, widthPercent = 20) {
  return new TableCell({
    width: { size: widthPercent, type: WidthType.PERCENTAGE },
    shading: isHeader
      ? { type: ShadingType.CLEAR, fill: '0A2540', color: 'FFFFFF' }
      : { type: ShadingType.CLEAR, fill: 'F8F9FA' },
    margins: { top: 120, bottom: 120, left: 150, right: 150 },
    children: [
      new Paragraph({
        children: [
          new TextRun({
            text,
            bold: isHeader,
            color: isHeader ? 'FFFFFF' : '222222',
            size: isHeader ? 20 : 18,
            font: 'Arial',
          }),
        ],
      }),
    ],
  });
}

function createHeading1(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_1,
    spacing: { before: 300, after: 140 },
    children: [
      new TextRun({
        text,
        bold: true,
        size: 28,
        color: '0A2540',
        font: 'Arial',
      }),
    ],
  });
}

function createHeading2(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_2,
    spacing: { before: 240, after: 100 },
    children: [
      new TextRun({
        text,
        bold: true,
        size: 24,
        color: '00558F',
        font: 'Arial',
      }),
    ],
  });
}

function createHeading3(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_3,
    spacing: { before: 180, after: 80 },
    children: [
      new TextRun({
        text,
        bold: true,
        size: 21,
        color: '333333',
        font: 'Arial',
      }),
    ],
  });
}

function createPara(text, boldPrefix = '') {
  return new Paragraph({
    spacing: { after: 120 },
    lineSpacing: 276, // 1.15 line spacing
    children: [
      boldPrefix
        ? new TextRun({ text: boldPrefix, bold: true, size: 20, font: 'Arial', color: '222222' })
        : null,
      new TextRun({ text, size: 20, font: 'Arial', color: '333333' }),
    ].filter(Boolean),
  });
}

function createBullet(text, boldPrefix = '') {
  return new Paragraph({
    bullet: { level: 0 },
    spacing: { after: 80 },
    children: [
      boldPrefix
        ? new TextRun({ text: boldPrefix, bold: true, size: 20, font: 'Arial', color: '222222' })
        : null,
      new TextRun({ text, size: 20, font: 'Arial', color: '333333' }),
    ].filter(Boolean),
  });
}

function createCallout(title, text) {
  return new Table({
    width: { size: 100, type: WidthType.PERCENTAGE },
    borders: {
      left: { style: BorderStyle.SINGLE, size: 24, color: 'B71C1C' },
      top: { style: BorderStyle.NONE },
      right: { style: BorderStyle.NONE },
      bottom: { style: BorderStyle.NONE },
    },
    rows: [
      new TableRow({
        children: [
          new TableCell({
            shading: { type: ShadingType.CLEAR, fill: 'FFEBEE' },
            margins: { top: 140, bottom: 140, left: 180, right: 180 },
            children: [
              new Paragraph({
                spacing: { after: 60 },
                children: [
                  new TextRun({ text: title, bold: true, size: 22, color: 'B71C1C', font: 'Arial' }),
                ],
              }),
              new Paragraph({
                children: [
                  new TextRun({ text, size: 19, color: '444444', font: 'Arial' }),
                ],
              }),
            ],
          }),
        ],
      }),
    ],
  });
}

async function buildDocument() {
  const doc = new Document({
    styles: {
      default: {
        document: {
          run: {
            font: 'Arial',
            size: 20,
            color: '333333',
          },
        },
      },
    },
    sections: [
      {
        properties: {
          page: {
            margin: { top: 1440, bottom: 1440, left: 1440, right: 1440 },
          },
        },
        headers: {
          default: new Header({
            children: [
              new Paragraph({
                alignment: AlignmentType.RIGHT,
                children: [
                  new TextRun({
                    text: 'CONFIDENTIAL // PROPRIETARY COMMERCIAL ASSET // IAA APP SUITE',
                    size: 16,
                    color: '888888',
                    font: 'Arial',
                  }),
                ],
              }),
            ],
          }),
        },
        footers: {
          default: new Footer({
            children: [
              new Paragraph({
                alignment: AlignmentType.CENTER,
                children: [
                  new TextRun({ text: 'Page ', size: 16, color: '888888', font: 'Arial' }),
                  new TextRun({ children: [PageNumber.CURRENT], size: 16, color: '888888', font: 'Arial' }),
                  new TextRun({ text: ' | BibiniiTech Ltd. - All Rights Reserved © 2026', size: 16, color: '888888', font: 'Arial' }),
                ],
              }),
            ],
          }),
        },
        children: [
          // ─── COVER PAGE ──────────────────────────────────────────────────────────
          new Paragraph({ spacing: { before: 1000 } }),
          new Paragraph({
            alignment: AlignmentType.CENTER,
            children: [
              new TextRun({
                text: 'INTERNAL AUDIT COMPLIANCE & STATUTORY REPORTING PLATFORM',
                bold: true,
                size: 36,
                color: '0A2540',
                font: 'Arial',
              }),
            ],
          }),
          new Paragraph({
            alignment: AlignmentType.CENTER,
            spacing: { before: 160, after: 400 },
            children: [
              new TextRun({
                text: 'Enterprise Public Financial Management (PFM), Statutory Audit Committee Oversight & National Defaulters Platform',
                italics: true,
                size: 22,
                color: '00558F',
                font: 'Arial',
              }),
            ],
          }),
          new Paragraph({
            alignment: AlignmentType.CENTER,
            spacing: { after: 600 },
            children: [
              new TextRun({
                text: 'CROSS-PLATFORM ARCHITECTURE SPECIFICATION & COMMERCIAL PROSPECTUS',
                bold: true,
                size: 22,
                color: 'B71C1C',
                font: 'Arial',
              }),
            ],
          }),

          createCallout(
            'STRICT PROPRIETARY WARNING & TRADE SECRET NOTICE',
            'This document contains confidential business trade secrets, algorithms, UI/UX designs, database models, and commercial architectures belonging exclusively to BibiniiTech Limited. Review of this document is granted strictly under the Non-Disclosure, Non-Circumvention, and Intellectual Property Reservation terms herein.'
          ),

          new Paragraph({ spacing: { before: 600 } }),
          new Table({
            width: { size: 100, type: WidthType.PERCENTAGE },
            rows: [
              new TableRow({
                children: [
                  createCell('Developer & IP Owner:', true, 35),
                  createCell('BibiniiTech Limited', false, 65),
                ],
              }),
              new TableRow({
                children: [
                  createCell('Release Version:', true, 35),
                  createCell('v2.4 Enterprise Production Release (2026)', false, 65),
                ],
              }),
              new TableRow({
                children: [
                  createCell('Platform Coverage:', true, 35),
                  createCell('Native Android (Jetpack Compose) + Enterprise Web (Next.js 16) + Cloud Backend', false, 65),
                ],
              }),
              new TableRow({
                children: [
                  createCell('Target Entity Scope:', true, 35),
                  createCell('466 Statutory Entities (147 MDAs, 42 SOEs, 16 RCCs, 261 MMDAs)', false, 65),
                ],
              }),
              new TableRow({
                children: [
                  createCell('Classification:', true, 35),
                  createCell('STRICTLY CONFIDENTIAL // COMMERCIAL IN CONFIDENCE', false, 65),
                ],
              }),
            ],
          }),

          new Paragraph({ children: [new PageBreak()] }),

          // ─── LEGAL CLAUSES: NDA, NON-CIRCUMVENTION & IP RIGHTS ───────────────────
          createHeading1('LEGAL PROTECTION: NON-DISCLOSURE, NON-CIRCUMVENTION & PROPRIETARY IP RIGHTS'),
          createPara(
            'This agreement governs the disclosure of all technical, operational, architectural, and commercial materials related to the Internal Audit Platform ("Platform") provided by BibiniiTech Limited ("Disclosing Party") to the prospective buyer, investor, or evaluating organization ("Recipient").'
          ),

          createHeading2('Clause 1: Definition of Confidential Information & Trade Secrets'),
          createPara(
            'Confidential Information encompasses all non-public information, whether communicated orally, visually, in electronic format, or in writing. This explicitly includes source code (Kotlin Jetpack Compose, TypeScript/Next.js), database schemas (Cloud Firestore collections, document structures, index designs), the automated statutory tri-state defaulters algorithm, the 4-tier deduplicated institutional mapping, dual-routing email architecture, security sentinel anti-tamper implementations, and commercial licensing fee structures.'
          ),

          createHeading2('Clause 2: Duty of Strict Confidentiality'),
          createBullet(
            ' The Recipient agrees to hold all materials in the strictest confidence, exercising at least the same degree of care as it exercises with its own most valuable proprietary assets, and in no event less than reasonable care.',
            'A. Standard of Care:'
          ),
          createBullet(
            ' Access to this document and associated repositories shall be limited strictly to directors, officers, and legal/technical advisors with a direct need-to-know, all of whom must be bound by written confidentiality obligations no less restrictive than those herein.',
            'B. Need-to-Know Limitation:'
          ),
          createBullet(
            ' The Recipient shall not disclose, photocopy, duplicate, summarize, or distribute any part of this technical specification without prior written authorization from BibiniiTech Limited.',
            'C. Prohibition of Dissemination:'
          ),

          createHeading2('Clause 3: Strict Non-Circumvention (Anti-Stealing & Direct Engagement Clause)'),
          createPara(
            'The Recipient explicitly covenants that it shall not, directly or indirectly, circumvent, bypass, or obviate BibiniiTech Limited in any transaction, procurement, or commercial engagement. Specifically, the Recipient shall not:'
          ),
          createBullet(
            'Directly or indirectly contact, pitch, solicit, or contract with the Internal Audit Agency (IAA), Ministry of Finance (MoF), Controller and Accountant-General’s Department (CAGD), Audit Service Ghana, Regional Coordinating Councils, or Assemblies regarding the digitized reporting, defaulters tracking, or tax calculator models disclosed herein.'
          ),
          createBullet(
            'Utilize any concept, architecture, data structure, or compliance workflow disclosed herein to develop, commission, or assist any third party in developing a competing or derivative software application.'
          ),
          createBullet(
            'This Non-Circumvention covenant shall remain in full force and effect for a period of five (5) consecutive years from the date of initial disclosure.'
          ),

          createHeading2('Clause 4: Full Intellectual Property Reservation & Anti-Reverse Engineering'),
          createPara(
            'All patents, copyrights, trademarks, source codes, database models, algorithms, and documentation remain the sole, absolute, and unencumbered intellectual property of BibiniiTech Limited. No license, express or implied, is granted by virtue of this disclosure. The Recipient shall not reverse-engineer, decompile, or reconstruct any binary or protocol implemented within the platform.'
          ),

          createHeading2('Clause 5: Legal Remedies, Injunction & Liquidated Damages'),
          createPara(
            'The Recipient acknowledges that any breach of this Agreement will cause immediate, irreparable, and irreparable harm to BibiniiTech Limited for which monetary damages alone would be inadequate. Consequently, BibiniiTech Limited shall be entitled to seek immediate injunctive relief, specific performance, and liquidated damages in a court of competent jurisdiction, alongside full recovery of legal costs, expert witness fees, and punitive damages.'
          ),

          new Paragraph({ children: [new PageBreak()] }),

          // ─── EXECUTIVE SUMMARY & REGULATORY CONTEXT ──────────────────────────────
          createHeading1('1. EXECUTIVE SUMMARY & STRATEGIC VALUE PROPOSITION'),
          createPara(
            'The Public Financial Management (PFM) landscape across emerging economies is undergoing rapid digital modernization. In Ghana, the Public Financial Management Act, 2016 (Act 921) and the Internal Audit Agency Act, 2003 (Act 658) establish stringent statutory obligations for public accountability.'
          ),
          createPara(
            'Every covered public entity—comprising 147 Ministries, Departments, and Agencies (MDAs), 42 State-Owned Enterprises (SOEs), 16 Regional Coordinating Councils (RCCs), and 261 Metropolitan, Municipal, and District Assemblies (MMDAs)—is mandated by law to submit timely quarterly audit reports, annual performance evaluations, implementation status statements, and audit committee reviews.'
          ),

          createHeading2('The Operational Crisis in Public Reporting'),
          createBullet(' Over 1,800+ mandatory statutory reports are filed annually via printed spiral documents, resulting in lost documents, physical transport bottlenecks, and severe oversight blind spots.', '1. Paper Bottlenecks:'),
          createBullet(' The regulatory agency previously lacked a centralized, real-time dashboard to instantly detect which public entities had defaulted on statutory deadlines.', '2. Absence of Live Tracking:'),
          createBullet(' District and regional auditors lacked unified access to PFM legislations, standardized audit committee templates, tax withholding matrices, and training opportunities.', '3. Resource Fragmentation:'),

          createHeading2('The Solution: The IAA Unified Compliance Ecosystem'),
          createPara(
            'The IAA App is a commercial-grade, multi-platform GovTech solution. It seamlessly couples an offline-resilient Native Android Application with an enterprise Next.js 16 Web Portal, powered by a secure cloud database and automated multi-stream email routing engine.'
          ),

          new Paragraph({ children: [new PageBreak()] }),

          // ─── COMPLETE SYSTEM ARCHITECTURE ─────────────────────────────────────────
          createHeading1('2. SYSTEM ARCHITECTURE & CROSS-PLATFORM DEPLOYMENT'),
          createPara(
            'The platform was engineered from the ground up following clean architecture, domain-driven design (DDD), and defense-in-depth security principles. Both mobile and web clients interface with a hardened cloud tier.'
          ),

          new Table({
            width: { size: 100, type: WidthType.PERCENTAGE },
            rows: [
              new TableRow({
                children: [
                  createCell('Architectural Tier', true, 25),
                  createCell('Android Native Client', true, 35),
                  createCell('Enterprise Web Portal', true, 40),
                ],
              }),
              new TableRow({
                children: [
                  createCell('Core Framework', false, 25),
                  createCell('Kotlin 2.0+ / Jetpack Compose / M3', false, 35),
                  createCell('Next.js 16.2 / React 19 / TypeScript', false, 40),
                ],
              }),
              new TableRow({
                children: [
                  createCell('State Architecture', false, 25),
                  createCell('MVVM / Coroutines StateFlow', false, 35),
                  createCell('React Server Components + Hooks', false, 40),
                ],
              }),
              new TableRow({
                children: [
                  createCell('Local Persistence', false, 25),
                  createCell('Room SQLite DB + DataStore', false, 35),
                  createCell('LocalStorage + In-Memory Cache', false, 40),
                ],
              }),
              new TableRow({
                children: [
                  createCell('Hardware Security', false, 25),
                  createCell('Biometrics + Root/Emulator Sentinel', false, 35),
                  createCell('Subresource Integrity + CSRF Defense', false, 40),
                ],
              }),
              new TableRow({
                children: [
                  createCell('Cloud Backend', false, 25),
                  createCell('Google Cloud Firestore (Sub-second sync)', false, 35),
                  createCell('Google Cloud Firestore (Sub-second sync)', false, 40),
                ],
              }),
              new TableRow({
                children: [
                  createCell('Transactional Relay', false, 25),
                  createCell('Brevo REST Relay + Cloud Functions', false, 35),
                  createCell('Brevo REST Relay + Serverless API Routes', false, 40),
                ],
              }),
            ],
          }),

          createHeading2('Lightweight Real-Time Synchronization Engine (syncToken Pattern)'),
          createPara(
            'To achieve instant cross-platform data synchronization without incurring astronomical cloud database read costs, the platform utilizes a proprietary syncToken architecture. Administrative modifications in deadlines, training seminars, notices, or email routes immediately bump a micro-timestamp document (`config/sync_state`). Mobile and web clients maintain a lightweight persistent listener on this single document; upon detecting a token increment, clients surgically invalidate local caches and re-fetch only modified data.'
          ),

          new Paragraph({ children: [new PageBreak()] }),

          // ─── COMPLETE FUNCTIONAL SPECIFICATIONS ──────────────────────────────────
          createHeading1('3. COMPREHENSIVE FUNCTIONAL SPECIFICATIONS'),

          createHeading2('3.1 Submissions Portal & Multi-Stream Distribution Engine'),
          createPara(
            'The Submissions Portal is the operational core of the platform, replacing paper-based statutory report delivery with authenticated digital filing.'
          ),
          createBullet('Filing creates a permanent, immutable record in Cloud Firestore complete with submitting auditor UID, institution affiliation, region, report type, and UTC timestamp.', 'A. Digital Audit Trail:'),
          createBullet('The system automatically attaches the uploaded audit files and dispatches transactional emails via Brevo relay to the central IAA desk, regional coordinating council (RCC) oversight desk, and institutional stakeholders simultaneously.', 'B. Dual Delivery Engine:'),
          createBullet('Quarterly IA Reports (Q1, Q2, Q3, Q4), Annual Workplans, Strategic Plans, Annual IA Performance Reports, Commitment Control Compliance (CCC) Reviews, Status of Implementation (SOI), and Special Audit Reports.', 'C. 6 Statutory Streams:'),
          createBullet('Strict client-side payload gatekeepers enforce a maximum of 5 files per filing and a 25MB total aggregated limit to guarantee transmission reliability over field networks.', 'D. Strict Payload Enforcement:'),
          createBullet('Users can begin drafting a submission offline or leave the form; drafts are automatically saved in local SQLite/LocalStorage and restored upon return.', 'E. Zero-Loss Draft Engine:'),

          createHeading2('3.2 Automated National Defaulters’ List & Compliance Registry'),
          createPara(
            'A national oversight mechanism that automatically audits reporting compliance across 466 statutory institutions.'
          ),
          createBullet('Reports whose statutory deadlines fall in the future display "Not Due Yet" with the scheduled deadline date. Defaulters and late lists remain hidden until the date elapses, preventing false public naming of compliant entities.', 'A. Dynamic Date-Elapse Gating:'),
          createBullet('Cleanly segregates institutions into Defaulters (unsubmitted past deadline), Late Submissions (submitted past deadline with exact days overdue), and On-Time Submissions.', 'B. Tri-State Compliance Classification:'),
          createBullet('Separates public institutions into 4 non-overlapping, deduplicated categories: MDAs (147), SOEs (42), RCCs (16), and MMDAs (261).', 'C. 4-Tier Public Entity Mapping:'),
          createBullet('Instant real-time search by institution name, filter by entity type, and drill down by 16 administrative regions.', 'D. Multidimensional Filtering:'),

          createHeading2('3.3 Dynamic Home Dashboard & Contextual Smart Compliance Badges'),
          createBullet('For practicing auditors, the Home screen inspects their institution’s submission record against active 30-day deadlines. If all upcoming reports are filed, it displays "00" with a green "All Caught Up" badge; otherwise, it displays the pending count with an amber "Due Soon" badge.', 'A. Hybrid Contextual Badge:'),
          createBullet('For administrators and guests, the badge dynamically shifts to display total system-wide "Upcoming Deadlines" falling due in the next 30 days.', 'B. Executive Overview Mode:'),
          createBullet('Tracks upcoming continuous professional development (CPD) training sessions within the next 30-day window.', 'C. Upcoming Seminars Counter:'),
          createBullet('Direct, one-click shortcuts that open pre-filled quarterly forms, CCC forms, or expand specific legislative libraries.', 'D. Deep-Linked Quick Access:'),

          createHeading2('3.4 National PFM Compliance League Table'),
          createPara(
            'A national benchmarking initiative fulfilling the Ministry of Finance Budget Statement mandate. The League Table evaluates and ranks MDAs, SOEs, and MMDAs on objective compliance metrics under PFM Act 921, promoting fiscal discipline and public accountability.'
          ),

          createHeading2('3.5 Interactive PFM Tax Calculators'),
          createPara('Embedded financial calculation engines complying with Ghana Revenue Authority (GRA) standards:'),
          createBullet('Computes graduated monthly tax brackets, automatic SSNIT 5.5% Tier 1 deduction, taxable gross, and take-home net pay.', '1. GRA Graduated PAYE Calculator:'),
          createBullet('Calculates withholding deductions across Goods (3%), Services/Consultancy (5% - 10%), Rent (8% / 15%), and Works (5%).', '2. Statutory Withholding Tax (WHT):'),
          createBullet('Calculates standard VAT (15%), NHIL (2.5%), GETFund (2.5%), and COVID-19 Health Recovery Levy (1%) compounded gross totals.', '3. Comprehensive VAT & Levies:'),

          createHeading2('3.6 Professional Trainings, Knowledge Base & Voting'),
          createBullet('Interactive booking hub for seminars with session times, venues/Zoom credentials, fees, and syllabus documents.', 'Training & Seminar Engine:'),
          createBullet('5-element matrix (Criteria, Condition, Cause, Consequence, Recommendation) providing standardized audit guidance.', 'Audit Findings & Criteria:'),
          createBullet('Authenticated, one-account-one-vote election engine for institutional AGMs and governance ballots.', 'Secure Stakeholder Voting:'),

          new Paragraph({ children: [new PageBreak()] }),

          // ─── ENTERPRISE SECURITY & ANTI-TAMPER SENTINEL ───────────────────────────
          createHeading1('4. ENTERPRISE SECURITY & ANTI-TAMPER ARCHITECTURE'),
          createPara(
            'Public financial infrastructure requires impenetrable security. The IAA Platform implements a multi-layered defense architecture:'
          ),

          createBullet('Scans system partitions for su binaries, test-keys, Magisk root modules, and dangerous system privileges on Android launch.', '1. Anti-Root Sentinel:'),
          createBullet('Blocks Java debuggers, dynamic analysis ptrace attachments, and reverse-engineering inspection tools.', '2. Debugger Attachment Blocker:'),
          createBullet('Detects virtualized emulator environments (QEMU, BlueStacks, Nox) to prevent automated penetration testing.', '3. Emulator Sandboxing Guard:'),
          createBullet('Verifies the cryptographic release signing SHA-256 certificate hash to immediately kill patched or modified APKs.', '4. Cryptographic Signature Integrity:'),
          createBullet('Multi-factor Biometric Prompt (fingerprint/face recognition) safeguards local draft access and submission filing.', '5. Hardware Biometrics:'),
          createBullet('Granular Role-Based Access Control (Admin, Auditor, Stakeholder, Guest) enforced via Cloud Firestore Security Rules.', '6. RBAC Governance:'),

          new Paragraph({ children: [new PageBreak()] }),

          // ─── COMMERCIAL OPPORTUNITY & BUYER ROI ───────────────────────────────────
          createHeading1('5. COMMERCIAL OPPORTUNITIES, MONETIZATION & BUYER ROI'),
          createPara(
            'The IAA Platform represents an exceptional high-growth commercial asset with multiple established monetization avenues:'
          ),

          new Table({
            width: { size: 100, type: WidthType.PERCENTAGE },
            rows: [
              new TableRow({
                children: [
                  createCell('Revenue Stream', true, 30),
                  createCell('Target Segment', true, 30),
                  createCell('Monetization Model', true, 40),
                ],
              }),
              new TableRow({
                children: [
                  createCell('GovTech Enterprise SaaS', false, 30),
                  createCell('466+ Covered Public Institutions', false, 30),
                  createCell('Annual recurring SaaS subscription per institution or central ministry oversight retainer.', false, 40),
                ],
              }),
              new TableRow({
                children: [
                  createCell('CPD Seminar & Training Fees', false, 30),
                  createCell('10,000+ Internal Auditors & Controllers', false, 30),
                  createCell('Transaction fee retention or direct billing on paid professional seminars and masterclasses.', false, 40),
                ],
              }),
              new TableRow({
                children: [
                  createCell('Audit Trail Verification', false, 30),
                  createCell('External Auditors & Parliamentary PAC', false, 30),
                  createCell('Premium access to cryptographically certified compliance logs and statutory filing receipts.', false, 40),
                ],
              }),
              new TableRow({
                children: [
                  createCell('Enterprise ERP Integrations', false, 30),
                  createCell('National Treasury & GIFMIS', false, 30),
                  createCell('Custom integration and API middleware licensing into national financial management systems.', false, 40),
                ],
              }),
            ],
          }),

          createHeading2('Geographic Extensibility Across Africa'),
          createPara(
            'The platform’s institutional hierarchy and submission engines are modular by design. While configured for Ghana’s PFM Act 921 framework, the entire platform can be white-labeled within 14 days for deployment across Nigeria, Kenya, Sierra Leone, Liberia, and Rwanda, where supreme internal audit institutions mandate equivalent statutory filings.'
          ),

          new Paragraph({ children: [new PageBreak()] }),

          // ─── ACQUISITION HANDOVER DELIVERABLES ────────────────────────────────────
          createHeading1('6. ACQUISITION DELIVERABLES & TECHNICAL HANDOVER'),
          createPara(
            'Upon execution of an Asset Purchase Agreement, BibiniiTech Limited will deliver a complete, turn-key technology package:'
          ),

          createBullet('Complete, clean, production-verified Android Studio project with full Gradle build configurations, vector assets, and Compose modules.', '1. Native Android Repository:'),
          createBullet('Complete Next.js 16 / React 19 / TypeScript web repository with optimized CSS modules and serverless routes.', '2. Enterprise Web Repository:'),
          createBullet('Production Firestore security rules, indexing schemas, storage bucket policies, and Cloud Function triggers.', '3. Hardened Cloud Architecture:'),
          createBullet('Verified, deduplicated JSON and TypeScript registries for all 147 MDAs, 42 SOEs, 16 RCCs, and 261 MMDAs.', '4. Master Data Assets:'),
          createBullet('Comprehensive deployment guides, environment variable blueprints, and administrative user manuals.', '5. Technical Documentation:'),
          createBullet('BibiniiTech Limited provides an optional 60-day post-acquisition engineering transition and advisory warranty.', '6. Transition Advisory:'),

          new Paragraph({ spacing: { before: 500 } }),
          new Table({
            width: { size: 100, type: WidthType.PERCENTAGE },
            borders: {
              top: { style: BorderStyle.SINGLE, size: 12, color: '0A2540' },
              bottom: { style: BorderStyle.SINGLE, size: 12, color: '0A2540' },
              left: { style: BorderStyle.NONE },
              right: { style: BorderStyle.NONE },
            },
            rows: [
              new TableRow({
                children: [
                  new TableCell({
                    margins: { top: 160, bottom: 160, left: 160, right: 160 },
                    children: [
                      new Paragraph({
                        alignment: AlignmentType.CENTER,
                        children: [
                          new TextRun({ text: 'COMMERCIAL INQUIRIES & ACQUISITION DISCUSSIONS', bold: true, size: 22, color: '0A2540' }),
                        ],
                      }),
                      new Paragraph({
                        alignment: AlignmentType.CENTER,
                        spacing: { before: 80 },
                        children: [
                          new TextRun({ text: 'BibiniiTech Limited | Proprietary Technology Asset', italics: true, size: 20, color: '555555' }),
                        ],
                      }),
                      new Paragraph({
                        alignment: AlignmentType.CENTER,
                        children: [
                          new TextRun({ text: 'Confidential Intellectual Property Prospectus – All Rights Reserved © 2026', size: 18, color: '777777' }),
                        ],
                      }),
                    ],
                  }),
                ],
              }),
            ],
          }),
        ],
      },
    ],
  });

  const buffer = await Packer.toBuffer(doc);

  const outPaths = [
    path.join(__dirname, 'IAA_App_Comprehensive_Product_Specification_and_Investor_Deck.docx'),
    path.join('C:', 'Users', 'GuyGuy', 'AndroidStudioProjects', 'IAAApp', 'IAA_App_Comprehensive_Product_Specification_and_Investor_Deck.docx'),
    path.join('C:', 'Users', 'GuyGuy', '.gemini', 'antigravity', 'brain', '55b667d4-52b8-4506-9a2b-5c3769ad0fdd', 'IAA_App_Comprehensive_Product_Specification_and_Investor_Deck.docx'),
  ];

  for (const outPath of outPaths) {
    fs.writeFileSync(outPath, buffer);
    console.log(`Document written successfully to: ${outPath}`);
  }
}

buildDocument().catch(console.error);

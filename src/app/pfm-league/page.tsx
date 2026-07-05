'use client';

import React, { useState, useEffect } from 'react';
import { doc, getDoc } from 'firebase/firestore';
import { getValue, fetchAndActivate } from 'firebase/remote-config';
import { db, remoteConfig } from '@/lib/firebase';
import styles from './pfm-league.module.css';

// ─── Data Types (matching Android) ─────────────────────────────────────────
interface PFMSection {
  title: string;
  status: string;
  institutions: string[];
}

// ─── Defaults (from Android's getDefaultMDAData() & getDefaultMMDAData()) ────
const DEFAULT_MDA_DATA: PFMSection[] = [
  {
    title: 'HIGHLY COMPLIANT INSTITUTIONS',
    status: 'HIGHLY COMPLIANT',
    institutions: [
      '1. Environmental Protection Authority',
      '2. Tema Oil Refinery',
      '3. Ministry of Energy and Green Transition',
      '4. Ghana National Petroleum Corporation',
      '5. Ministry of Finance',
      '6. Ghana Aids Commission',
      '7. Petroleum Hub Development Corporation',
    ],
  },
  {
    title: 'COMPLIANT INSTITUTIONS',
    status: 'COMPLIANT',
    institutions: [
      '8. Cocoa Marketing Company',
      '9. Petroleum Commission',
      '10. Office of the Registrar of Companies',
      '11. Ministry of Food and Agriculture',
      '12. Office of the Attorney-General and Ministry of Justice',
      '13. Ministry of Transport',
      '14. Commission on Human Rights and Administrative Justice',
      '15. Ministry of Communication Digital Technology and Innovation',
      '16. Office of the Head of the Local Government Service',
      '17. Ghana Airports Company Limited',
      '18. Ghana Education Trust Fund (GETFund)',
      '19. Forestry Commission',
      '20. National Pensions Regulatory Authority',
      '21. Ministry of Foreign Affairs',
      '22. National Service Secretariat',
      '23. Ghana Highway Authority',
      '24. Ghana Standards Authority',
      '25. Ministry of Fisheries and Aquaculture',
      '26. Ministry of Youth Development and Empowerment',
      '27. Ghana News Agency',
      '28. Council for Scientific and Industrial Research',
      '29. Rent Control Department',
      '30. Public Services Commission',
      '31. Ghana Shippers Authority',
      '32. National Insurance Commission',
      '33. Tema Development Corporation (TDC)',
      '34. Minerals Commission',
      '35. Ghana Police Service',
      '36. National Commission for Civic Education',
      '37. Right to Information Commission',
      '38. Office of the President, Jubilee House',
      '39. Institute of Local Government Studies',
      '40. Driver and Vehicle Licensing Authority',
      '41. Ministry of Environment, Science and Technology',
      '42. National Identification Authority',
      '43. Copyright Office',
      '44. Narcotics Control Commission',
      '45. National Petroleum Authority',
      '46. Ministry of Trade, Agribusiness and Industry',
      '47. Ghana Export Promotion Authority',
      '48. Ministry of Tourism, Culture and Creative Arts',
      '49. Ministry of Works, Housing, and Water Resources',
      '50. Ministry of Health',
      '51. Securities and Exchange Commission',
      '52. National Intelligence Bureau',
      '53. Ghana Atomic Energy Commission',
      '54. Controller & Accountant - General\'s Dept',
      '55. Cyber Security Authority',
      '56. Zongo Development Fund',
      '57. National Youth Authority',
      '58. Ministry of Local Government, Chieftaincy and Religious Affairs',
      '59. Ghana College of Physicians and Surgeons',
      '60. Ghana Immigration Service',
    ],
  },
  {
    title: 'MODERATELY COMPLIANT INSTITUTIONS',
    status: 'MODERATELY COMPLIANT',
    institutions: [
      '61. Department of Parks and Gardens',
      '62. Registrar-General’s Dept.',
      '63. Ghana Infrastructure and Investment Fund',
      '64. Ministry for Sports and Recreation',
      '65. Ghana Gas',
      '66. Ministry of Defence',
      '67. District Assemblies Common Fund',
      '68. Land Use and Spatial Planning Authority (LUSPA)',
      '69. National Labour Commission',
      '70. Ministry of Gender, Children and Social Protection',
      '71. Ministry of Labour, Jobs, and Employment',
      '72. Ghana Ports and Harbours Authority',
      '73. Ghana Health Service',
      '74. Ghana Prisons Service',
      '75. Metro Mass Transit Ltd.',
      '76. Ministry of Interior',
      '77. Cocoa Processing Company',
      '78. Ministry of Roads and Highways',
      '79. Economic and Organised Crime Office',
      '80. University for Professional Studies',
      '81. National Development Planning Commission',
      '82. Ghana Audit Service',
    ],
  },
  {
    title: 'LEAST COMPLIANT INSTITUTIONS',
    status: 'LEAST COMPLIANT',
    institutions: [
      '83. Office of Legal Aid Commission',
      '84. Minerals Income Investment Fund',
      '85. Ghana Commodity Exchange',
      '86. Venture Capital Trust Fund',
      '87. National Peace Council',
      '88. Financial Intelligence Centre (FIC)',
      '89. Korle Bu Teaching Hospital',
      '90. University of Ghana',
      '91. Ghana Integrated Iron & Steel Development Corporation',
      '92. National Council for Curriculum & Assessment',
      '93. Ghana Revenue Authority',
      '94. National Media Commission',
      '95. National Food Buffer Stock Company',
      '96. Office of the Head of the Civil Service',
      '97. Ministry of Education - HQ',
      '98. Ghana Infrastructure Fund for Electronic Communications',
      '99. Ghana National Fire Service',
      '100. NADMO',
      '101. National Communications Authority',
    ],
  },
];

const DEFAULT_MMDA_DATA: PFMSection[] = [
  { title: 'HIGHLY COMPLIANT INSTITUTIONS', status: 'HIGHLY COMPLIANT', institutions: [] },
  { title: 'COMPLIANT INSTITUTIONS', status: 'COMPLIANT', institutions: [] },
  { title: 'MODERATELY COMPLIANT INSTITUTIONS', status: 'MODERATELY COMPLIANT', institutions: [] },
  { title: 'LEAST COMPLIANT INSTITUTIONS', status: 'LEAST COMPLIANT', institutions: [] },
];

const DEFAULT_HEADING = 'PFM Compliance League Table';
const DEFAULT_DESCRIPTION =
  'The Public Financial Management (PFM) Compliance League Table is a landmark transparency and accountability initiative aimed at strengthening fiscal discipline and improving the management of public resources. This fulfills the Government’s commitment in the 2025 Budget Statement to publish an objective, evidence-based assessment of how public institutions comply with the PFM Act, 2016 (Act 921), its Regulations and associated laws. The League Table serves as a performance benchmarking tool that measures the extent to which public institutions adhere to the rules and procedures governing the use of public funds.';

// ─── Main Component ────────────────────────────────────────────────────────
export default function PFMLeaguePage() {
  const [heading, setHeading] = useState(DEFAULT_HEADING);
  const [description, setDescription] = useState(DEFAULT_DESCRIPTION);
  const [mdaData, setMdaData] = useState<PFMSection[]>(DEFAULT_MDA_DATA);
  const [mmdaData, setMmdaData] = useState<PFMSection[]>(DEFAULT_MMDA_DATA);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function initPfmData() {
      // 1. Initialize and Fetch Remote Config if available (Browser only)
      let rcHeading = '';
      let rcDescription = '';
      let rcMdas = '';
      let rcMmdas = '';

      if (remoteConfig) {
        try {
          // Set standard settings/intervals
          remoteConfig.settings.minimumFetchIntervalMillis = 3600000; // 1 hour
          await fetchAndActivate(remoteConfig);
          rcHeading = getValue(remoteConfig, 'pfm_league_heading').asString();
          rcDescription = getValue(remoteConfig, 'pfm_league_description').asString();
          rcMdas = getValue(remoteConfig, 'pfm_league_mdas').asString();
          rcMmdas = getValue(remoteConfig, 'pfm_league_mmdas').asString();
        } catch (e) {
          console.warn('Failed to load Remote Config values:', e);
        }
      }

      // 2. Fetch Firestore Data (same priority check as Android)
      let firestoreMdaJson = '';
      let firestoreMmdaJson = '';
      let firestoreHeading = '';
      let firestoreDescription = '';

      try {
        const [mdaSnap, mmdaSnap, headingSnap, descSnap] = await Promise.all([
          getDoc(doc(db, 'config', 'pfm_league_mda')),
          getDoc(doc(db, 'config', 'pfm_league_mmda')),
          getDoc(doc(db, 'config', 'pfm_league_heading')),
          getDoc(doc(db, 'config', 'pfm_league_description')),
        ]);

        if (mdaSnap.exists()) firestoreMdaJson = mdaSnap.data().json || '';
        if (mmdaSnap.exists()) firestoreMmdaJson = mmdaSnap.data().json || '';
        if (headingSnap.exists()) firestoreHeading = headingSnap.data().value || '';
        if (descSnap.exists()) firestoreDescription = descSnap.data().value || '';
      } catch (e) {
        console.warn('Failed to load Firestore config docs:', e);
      }

      // Resolve Heading & Description
      setHeading(firestoreHeading || rcHeading || DEFAULT_HEADING);
      setDescription(firestoreDescription || rcDescription || DEFAULT_DESCRIPTION);

      // Resolve MDA Data
      if (firestoreMdaJson) {
        setMdaData(parsePfmJson(firestoreMdaJson, DEFAULT_MDA_DATA));
      } else if (rcMdas) {
        setMdaData(parsePfmJson(rcMdas, DEFAULT_MDA_DATA));
      } else {
        setMdaData(DEFAULT_MDA_DATA);
      }

      // Resolve MMDA Data
      if (firestoreMmdaJson) {
        setMmdaData(parsePfmJson(firestoreMmdaJson, DEFAULT_MMDA_DATA));
      } else if (rcMmdas) {
        setMmdaData(parsePfmJson(rcMmdas, DEFAULT_MMDA_DATA));
      } else {
        setMmdaData(DEFAULT_MMDA_DATA);
      }

      setLoading(false);
    }

    initPfmData();
  }, []);

  function parsePfmJson(jsonStr: string, fallback: PFMSection[]): PFMSection[] {
    try {
      if (!jsonStr.trim()) return fallback;
      const parsed = JSON.parse(jsonStr);
      return Array.isArray(parsed) ? parsed : fallback;
    } catch {
      return fallback;
    }
  }

  if (loading) {
    return <div className={styles.loading}>Loading PFM League Table...</div>;
  }

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div className={styles.iconContainer}>
          <svg width="48" height="48" viewBox="0 0 24 24" fill="currentColor">
            <path d="M16 11V3H8v8H2v10h20V11h-6zM10 5h4v14h-4V5zm-6 8h4v6H4v-6zm16 6h-4v-6h4v6z" />
          </svg>
        </div>
        <h1>{heading}</h1>
        <p>{description}</p>
      </header>

      <div className={styles.categoryList}>
        <LeagueCategory title="MDAs (Ministries, Departments & Agencies)" sections={mdaData} />
        <LeagueCategory title="MMDAs (Metropolitan, Municipal & District Assemblies)" sections={mmdaData} />
      </div>

      <div style={{ height: 100 }} />
    </div>
  );
}

// ─── Expandable Category Component ──────────────────────────────────────────
function LeagueCategory({ title, sections }: { title: string; sections: PFMSection[] }) {
  const [expanded, setExpanded] = useState(false);

  return (
    <div className={styles.categoryCard}>
      <button
        className={styles.categoryHeader}
        onClick={() => setExpanded(!expanded)}
        aria-expanded={expanded}
      >
        <h2>{title}</h2>
        <span
          className={styles.chevron}
          style={{ transform: expanded ? 'rotate(180deg)' : 'none' }}
        >
          ▼
        </span>
      </button>

      {expanded && (
        <div className={styles.categoryContent}>
          {sections.map((section, idx) => (
            <PFMSectionView key={idx} section={section} />
          ))}
        </div>
      )}
    </div>
  );
}

// ─── Status Section View (mirrors PFMSectionView in Android) ────────────────
function PFMSectionView({ section }: { section: PFMSection }) {
  const statusLower = section.status.toUpperCase();

  let statusClass = styles.statusDefault;
  if (statusLower.includes('HIGHLY COMPLIANT')) {
    statusClass = styles.statusHighlyCompliant;
  } else if (statusLower === 'COMPLIANT') {
    statusClass = styles.statusCompliant;
  } else if (statusLower.includes('MODERATELY COMPLIANT')) {
    statusClass = styles.statusModeratelyCompliant;
  } else if (statusLower.includes('LEAST COMPLIANT')) {
    statusClass = styles.statusLeastCompliant;
  }

  return (
    <div className={styles.sectionContainer}>
      <h3 className={styles.sectionTitle}>{section.title}</h3>
      <div className={`${styles.institutionBox} ${statusClass}`}>
        {section.institutions.length === 0 ? (
          <p className={styles.emptyText}>No institutions listed yet.</p>
        ) : (
          <ul className={styles.institutionList}>
            {section.institutions.map((inst, idx) => (
              <li key={idx} className={styles.institutionItem}>
                {inst}
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}

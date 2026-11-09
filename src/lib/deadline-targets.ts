import { REGIONS, MMDA_DATA, RCC_DATA } from '@/data/mmda_data';
import { MDA_DATA, SOE_DATA } from '@/data/mda_data';

export type DeadlineTargetModel = {
  report: string;
  dateString: string;
  targetGroup?: string;
  targetGroups?: string[];
  targetInstitutions?: string[];
};

export interface ResolvedInstitution {
  name: string;
  type: string;
  region: string;
}

/**
 * Resolves an institution's canonical type and region from its name and reference datasets.
 */
export function resolveInstitution(
  name: string,
  fallbackType: string = '',
  fallbackRegion: string = ''
): ResolvedInstitution {
  const trimmed = (name || '').trim();
  const lower = trimmed.toLowerCase();

  // 1. RCCs check
  const rccRegion = REGIONS.find((r) => RCC_DATA[r]?.toLowerCase() === lower);
  if (rccRegion || lower.endsWith(' rcc')) {
    const reg = rccRegion || trimmed.replace(/\s+rcc$/i, '').trim();
    return { name: trimmed, type: 'RCC', region: reg };
  }

  // 2. MMDAs check
  for (const [region, list] of Object.entries(MMDA_DATA)) {
    if (list.some((m) => m.toLowerCase() === lower)) {
      return { name: trimmed, type: 'MMDA', region };
    }
  }

  // 3. MDAs check
  if (MDA_DATA.some((m) => m.toLowerCase() === lower)) {
    return { name: trimmed, type: 'MDA', region: fallbackRegion };
  }

  // 4. SOEs check
  if (SOE_DATA.some((s) => s.toLowerCase() === lower)) {
    return { name: trimmed, type: 'SOE', region: fallbackRegion };
  }

  return { name: trimmed, type: fallbackType, region: fallbackRegion };
}

/**
 * Evaluates whether a deadline applies to a specific institution based on its target filters.
 * Supports:
 * - "ALL": All institutions nationwide
 * - "TYPE:MDA", "TYPE:SOE", "TYPE:RCC", "TYPE:MMDA"
 * - "REGION_ALL:<Region>": All institutions within that region
 * - "REGION_MDA:<Region>": MDAs within that region
 * - "REGION_MMDA:<Region>": MMDAs within that region
 * - "targetInstitutions": Exact match against specified institution names
 * - Legacy "targetGroup": Backward compatible fallback
 */
export function isDeadlineApplicable(
  deadline: DeadlineTargetModel,
  instName: string,
  fallbackType: string = '',
  fallbackRegion: string = ''
): boolean {
  const { type, region } = resolveInstitution(instName, fallbackType, fallbackRegion);
  const normName = instName.trim().toLowerCase();
  const typeUpper = (type || fallbackType || '').toUpperCase().trim();
  const regionTrimmed = (region || fallbackRegion || '').trim();

  // 1. Specific institution check (priority match)
  if (deadline.targetInstitutions && deadline.targetInstitutions.length > 0) {
    if (deadline.targetInstitutions.some((inst) => inst.trim().toLowerCase() === normName)) {
      return true;
    }
  }

  const groups = deadline.targetGroups || [];

  // 2. Fallback to legacy single targetGroup if no targetGroups & no targetInstitutions are set
  if (groups.length === 0 && (!deadline.targetInstitutions || deadline.targetInstitutions.length === 0)) {
    const legacy = (deadline.targetGroup || 'ALL').toUpperCase().trim();
    if (!legacy || legacy === 'ALL') return true;
    return typeUpper ? legacy === typeUpper : true;
  }

  // 3. ALL filter check
  if (groups.includes('ALL')) return true;

  // 4. National institution category / type check
  if (typeUpper) {
    if (groups.includes(`TYPE:${typeUpper}`) || groups.includes(typeUpper)) return true;
  }

  // 5. Regional filters check
  if (regionTrimmed) {
    // All institutions in this region
    if (groups.includes(`REGION_ALL:${regionTrimmed}`)) return true;
    // Specific type in this region (e.g. MMDAs in Ashanti, MDAs in Ashanti)
    if (typeUpper && groups.includes(`REGION_${typeUpper}:${regionTrimmed}`)) return true;
  }

  return false;
}

/**
 * Generates a concise human-readable summary of the deadline's target filters.
 */
export function formatTargetSummary(deadline: {
  targetGroup?: string;
  targetGroups?: string[];
  targetInstitutions?: string[];
}): string {
  const groups = deadline.targetGroups || [];
  const insts = deadline.targetInstitutions || [];

  if (groups.includes('ALL') || (groups.length === 0 && insts.length === 0)) {
    if (deadline.targetGroup && deadline.targetGroup !== 'ALL') {
      switch (deadline.targetGroup.toUpperCase()) {
        case 'MDA':
          return 'MDAs Only';
        case 'SOE':
          return 'SOEs Only';
        case 'RCC':
          return 'RCCs Only';
        case 'MMDA':
          return 'MMDAs Only';
        default:
          return deadline.targetGroup;
      }
    }
    return 'All Institutions';
  }

  const parts: string[] = [];

  for (const g of groups) {
    if (g === 'TYPE:MDA') parts.push('All MDAs');
    else if (g === 'TYPE:SOE') parts.push('All SOEs');
    else if (g === 'TYPE:RCC') parts.push('All RCCs');
    else if (g === 'TYPE:MMDA') parts.push('All MMDAs');
    else if (g.startsWith('REGION_ALL:')) parts.push(`${g.replace('REGION_ALL:', '')} (All)`);
    else if (g.startsWith('REGION_MDA:')) parts.push(`${g.replace('REGION_MDA:', '')} (MDAs)`);
    else if (g.startsWith('REGION_MMDA:')) parts.push(`${g.replace('REGION_MMDA:', '')} (MMDAs)`);
    else if (g.startsWith('REGION_RCC:')) parts.push(`${g.replace('REGION_RCC:', '')} (RCC)`);
    else parts.push(g);
  }

  if (insts.length === 1) {
    parts.push(insts[0]);
  } else if (insts.length > 1) {
    parts.push(`${insts.length} specific institutions`);
  }

  return parts.length > 0 ? parts.join(', ') : 'All Institutions';
}

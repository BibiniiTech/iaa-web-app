import { doc, getDoc } from 'firebase/firestore';
import { db } from './firebase';

export interface CloudConfig {
  mda_to?: string[];
  mda_cc?: string[];
  mmda_to?: string[];
  region_cc?: Record<string, string[]>;
  mmda_institution_cc?: Record<string, Record<string, string[]>>;
}

const DEFAULT_RECIPIENT = "clemzy93@gmail.com";

const DEFAULT_EMAIL_CONFIGS: Record<string, CloudConfig> = {
  quarterly: {
    mda_to: [DEFAULT_RECIPIENT],
    mda_cc: [],
    mmda_to: [DEFAULT_RECIPIENT],
    region_cc: {},
    mmda_institution_cc: {},
  },
  annual: {
    mda_to: [DEFAULT_RECIPIENT],
    mda_cc: [],
    mmda_to: [DEFAULT_RECIPIENT],
    region_cc: {},
    mmda_institution_cc: {},
  },
  ccc: {
    mda_to: [DEFAULT_RECIPIENT],
    mda_cc: [],
    mmda_to: [DEFAULT_RECIPIENT],
    region_cc: {},
  },
  soi: {
    mda_to: [DEFAULT_RECIPIENT],
    mda_cc: [],
    mmda_to: [DEFAULT_RECIPIENT],
    region_cc: {},
  },
  other: {
    mda_to: [DEFAULT_RECIPIENT],
    mda_cc: [],
    mmda_to: [DEFAULT_RECIPIENT],
    region_cc: {},
  },
};

let cachedConfigJson: string | null = null;

async function getCloudData(category: string): Promise<CloudConfig> {
  if (cachedConfigJson === null) {
    try {
      const configDoc = await getDoc(doc(db, 'config', 'recipient_emails'));
      cachedConfigJson = configDoc.exists() ? String(configDoc.data().json || "") : "";
    } catch (error) {
      console.error("Failed to fetch recipient_emails from Firestore", error);
      cachedConfigJson = "";
    }
  }

  if (cachedConfigJson) {
    try {
      const parsed = JSON.parse(cachedConfigJson);
      if (isRecord(parsed)) {
        if (category in parsed) {
          return normalizeCloudConfig(parsed[category]);
        }
      }
    } catch {}

    try {
      return normalizeCloudConfig(JSON.parse(cachedConfigJson));
    } catch (error) {
      console.error("Failed to parse recipient config JSON", error);
    }
  }

  return DEFAULT_EMAIL_CONFIGS[category] || {};
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return !!value && typeof value === 'object' && !Array.isArray(value);
}

function getStringList(value: unknown): string[] {
  if (!Array.isArray(value)) return [];
  return value
    .filter((email): email is string => typeof email === 'string')
    .map(email => email.trim())
    .filter(Boolean);
}

function getStringListMap(value: unknown): Record<string, string[]> {
  if (!isRecord(value)) return {};
  return Object.fromEntries(
    Object.entries(value).map(([key, list]) => [key, getStringList(list)])
  );
}

function getNestedStringListMap(value: unknown): Record<string, Record<string, string[]>> {
  if (!isRecord(value)) return {};
  return Object.fromEntries(
    Object.entries(value).map(([region, institutions]) => [
      region,
      isRecord(institutions) ? getStringListMap(institutions) : {},
    ])
  );
}

function normalizeCloudConfig(value: unknown): CloudConfig {
  if (!isRecord(value)) return {};

  return {
    mda_to: getStringList(value.mda_to),
    mda_cc: getStringList(value.mda_cc),
    mmda_to: getStringList(value.mmda_to),
    region_cc: getStringListMap(value.region_cc),
    mmda_institution_cc: getNestedStringListMap(value.mmda_institution_cc),
  };
}

export async function getMdaTo(category: string): Promise<string[]> {
  const cloudData = await getCloudData(category);
  return cloudData.mda_to || [];
}

export async function getMdaCc(category: string): Promise<string[]> {
  const cloudData = await getCloudData(category);
  return cloudData.mda_cc || [];
}

export async function getMmdaTo(category: string): Promise<string[]> {
  const cloudData = await getCloudData(category);
  return cloudData.mmda_to || [];
}

export async function getRegionCc(category: string, regionName: string): Promise<string[]> {
  const cloudData = await getCloudData(category);
  return cloudData.region_cc?.[regionName] || [];
}

export async function getMmdaInstitutionCc(category: string, regionName: string, institutionName: string): Promise<string[]> {
  const cloudData = await getCloudData(category);
  return cloudData.mmda_institution_cc?.[regionName]?.[institutionName] || [];
}

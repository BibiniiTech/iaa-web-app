import {
  collection,
  doc,
  DocumentData,
  getDoc,
  getDocs,
  limit,
  orderBy,
  query,
} from 'firebase/firestore';
import { db } from './firebase';

const CACHE_TTL_MS = 30 * 60 * 1000; // 30 minutes
const SESSION_PREFIX = 'iaa_cache_v1_';

interface CacheEntry<T> {
  value: T;
  timestamp: number;
}

export interface CachedDbDocument {
  id: string;
  name: string;
  category: string;
  downloadUrl: string;
  timestamp: number;
}

export interface CachedVotingResults {
  categoryTotals: Record<string, number>;
  optionCounts: Record<string, number>;
}

const memoryCache = new Map<string, CacheEntry<unknown>>();
const inFlightRequests = new Map<string, Promise<unknown>>();

function readFromCache<T>(key: string, ttlMs: number = CACHE_TTL_MS): T | undefined {
  const now = Date.now();
  const mem = memoryCache.get(key);
  if (mem && now - mem.timestamp < ttlMs) {
    return mem.value as T;
  }

  if (typeof window !== 'undefined') {
    try {
      const raw = sessionStorage.getItem(SESSION_PREFIX + key);
      if (raw) {
        const parsed = JSON.parse(raw) as CacheEntry<T>;
        if (parsed && typeof parsed.timestamp === 'number' && now - parsed.timestamp < ttlMs) {
          memoryCache.set(key, parsed);
          return parsed.value;
        }
      }
    } catch {
      // Ignore sessionStorage errors
    }
  }

  return undefined;
}

function writeToCache<T>(key: string, value: T): void {
  const entry: CacheEntry<T> = { value, timestamp: Date.now() };
  memoryCache.set(key, entry);
  if (typeof window !== 'undefined') {
    try {
      sessionStorage.setItem(SESSION_PREFIX + key, JSON.stringify(entry));
    } catch {
      // Ignore quota or storage errors
    }
  }
}

function removeFromCache(key: string): void {
  memoryCache.delete(key);
  if (typeof window !== 'undefined') {
    try {
      sessionStorage.removeItem(SESSION_PREFIX + key);
    } catch {
      // Ignore storage errors
    }
  }
}

async function deduplicatedFetch<T>(
  key: string,
  fetcher: () => Promise<T>,
  options?: { forceRefresh?: boolean; ttlMs?: number }
): Promise<T> {
  const forceRefresh = options?.forceRefresh ?? false;
  const ttlMs = options?.ttlMs ?? CACHE_TTL_MS;

  if (!forceRefresh) {
    const cached = readFromCache<T>(key, ttlMs);
    if (cached !== undefined) {
      return cached;
    }
  }

  const existingPromise = inFlightRequests.get(key);
  if (existingPromise && !forceRefresh) {
    return existingPromise as Promise<T>;
  }

  const promise = fetcher()
    .then((result) => {
      writeToCache(key, result);
      return result;
    })
    .finally(() => {
      inFlightRequests.delete(key);
    });

  inFlightRequests.set(key, promise);
  return promise;
}

/**
 * Fetches and caches `users/{uid}` across Navigation, Home, Submissions, Voting, Profile, and Admin pages.
 */
export async function getCachedUserProfile(
  uid: string,
  forceRefresh = false
): Promise<DocumentData | null> {
  if (!uid) return null;
  const key = `user_profile_${uid}`;
  return deduplicatedFetch(
    key,
    async () => {
      const snap = await getDoc(doc(db, 'users', uid));
      return snap.exists() ? snap.data() : null;
    },
    { forceRefresh }
  );
}

export function setCachedUserProfile(uid: string, data: DocumentData): void {
  if (!uid) return;
  writeToCache(`user_profile_${uid}`, data);
}

export function clearCachedUserProfile(uid?: string): void {
  if (uid) {
    removeFromCache(`user_profile_${uid}`);
    removeFromCache(`has_voted_${uid}`);
  }
}

/**
 * Fetches and caches any `config/{docId}` document across pages.
 */
export async function getCachedConfigDoc<T = Record<string, unknown>>(
  docId: string,
  forceRefresh = false
): Promise<T | null> {
  const key = `config_doc_${docId}`;
  return deduplicatedFetch(
    key,
    async () => {
      const snap = await getDoc(doc(db, 'config', docId));
      return snap.exists() ? (snap.data() as T) : null;
    },
    { forceRefresh }
  );
}

export function setCachedConfigDoc<T = Record<string, unknown>>(docId: string, data: T): void {
  writeToCache(`config_doc_${docId}`, data);
}

/**
 * Fetches and caches the bounded list of `portal_documents` (limit 200) once per session,
 * shared across `/resources`, `/trainings`, and `/admin`.
 */
export async function getCachedPortalDocuments(
  forceRefresh = false
): Promise<CachedDbDocument[]> {
  const key = 'portal_documents_all';
  return deduplicatedFetch(
    key,
    async () => {
      const q = query(
        collection(db, 'portal_documents'),
        orderBy('timestamp', 'desc'),
        limit(200)
      );
      const snap = await getDocs(q);
      return snap.docs.map(
        (d) =>
          ({
            id: d.id,
            ...d.data(),
          }) as CachedDbDocument
      );
    },
    { forceRefresh }
  );
}

export function invalidatePortalDocumentsCache(): void {
  removeFromCache('portal_documents_all');
}

/**
 * Checks whether `votes/{uid}` exists, caching the boolean result.
 */
export async function getCachedVotingStatus(
  uid: string,
  forceRefresh = false
): Promise<boolean> {
  if (!uid) return false;
  const key = `has_voted_${uid}`;
  return deduplicatedFetch(
    key,
    async () => {
      const snap = await getDoc(doc(db, 'votes', uid));
      return snap.exists();
    },
    { forceRefresh }
  );
}

export function setCachedVotingStatus(uid: string, hasVoted: boolean): void {
  if (!uid) return;
  writeToCache(`has_voted_${uid}`, hasVoted);
}

/**
 * Fetches and caches `config/voting_results` (5-minute TTL for fresh tallies without listener storms).
 */
export async function getCachedVotingResults(
  forceRefresh = false
): Promise<CachedVotingResults> {
  const key = 'voting_results_summary';
  return deduplicatedFetch(
    key,
    async () => {
      const snap = await getDoc(doc(db, 'config', 'voting_results'));
      const catTotals: Record<string, number> = {};
      const optCounts: Record<string, number> = {};
      if (snap.exists()) {
        const data = snap.data();
        Object.entries(data).forEach(([k, val]) => {
          const num = Number(val) || 0;
          if (k.startsWith('cat_')) catTotals[k.replace('cat_', '')] = num;
          if (k.startsWith('opt_')) optCounts[k.replace('opt_', '')] = num;
        });
      }
      return { categoryTotals: catTotals, optionCounts: optCounts };
    },
    { forceRefresh, ttlMs: 5 * 60 * 1000 }
  );
}

export function invalidateVotingResultsCache(): void {
  removeFromCache('voting_results_summary');
}

/**
 * Clears all cached config and document entries (used on admin save or sign-out).
 */
export function invalidateAllCaches(): void {
  memoryCache.clear();
  inFlightRequests.clear();
  if (typeof window !== 'undefined') {
    try {
      const keysToRemove: string[] = [];
      for (let i = 0; i < sessionStorage.length; i++) {
        const k = sessionStorage.key(i);
        if (k && k.startsWith(SESSION_PREFIX)) {
          keysToRemove.push(k);
        }
      }
      keysToRemove.forEach((k) => sessionStorage.removeItem(k));
    } catch {
      // Ignore storage errors
    }
  }
}

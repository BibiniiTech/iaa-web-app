import { initializeApp, getApps, getApp } from "firebase/app";
import { getAuth } from "firebase/auth";
import {
  getFirestore,
  initializeFirestore,
  persistentLocalCache,
  persistentMultipleTabManager,
} from "firebase/firestore";
import { getStorage } from "firebase/storage";
import { getRemoteConfig } from "firebase/remote-config";

const firebaseConfig = {
  apiKey: process.env.NEXT_PUBLIC_FIREBASE_API_KEY,
  authDomain: process.env.NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN,
  projectId: process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID,
  storageBucket: process.env.NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: process.env.NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID,
  appId: process.env.NEXT_PUBLIC_FIREBASE_APP_ID,
};

// Next.js build-time fallback to prevent "auth/invalid-api-key" error
const isFirstInit = getApps().length === 0;
const app = !isFirstInit
  ? getApp()
  : initializeApp({
      ...firebaseConfig,
      apiKey: firebaseConfig.apiKey || "AIzaSy-DummyKey-For-Build-Time"
    });

const auth = getAuth(app);

let db: ReturnType<typeof getFirestore>;
if (typeof window !== "undefined" && isFirstInit) {
  try {
    db = initializeFirestore(app, {
      localCache: persistentLocalCache({
        tabManager: persistentMultipleTabManager(),
      }),
    });
  } catch {
    db = getFirestore(app);
  }
} else {
  db = getFirestore(app);
}

const storage = getStorage(app);

// Remote Config is browser-only (3-hour minimum fetch interval)
const remoteConfig = typeof window !== "undefined" ? getRemoteConfig(app) : null;
if (remoteConfig) {
  remoteConfig.settings.minimumFetchIntervalMillis = 10800 * 1000;
}

export { app, auth, db, storage, remoteConfig };

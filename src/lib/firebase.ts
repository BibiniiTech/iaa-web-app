import { initializeApp, getApps, getApp } from "firebase/app";
import { getAuth } from "firebase/auth";
import { getFirestore } from "firebase/firestore";
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
const isBuildTime = typeof window === "undefined" && !process.env.NEXT_PUBLIC_FIREBASE_API_KEY;

const app = getApps().length > 0
  ? getApp()
  : initializeApp({
      ...firebaseConfig,
      apiKey: firebaseConfig.apiKey || "AIzaSy-DummyKey-For-Build-Time"
    });

const auth = getAuth(app);
const db = getFirestore(app);
const storage = getStorage(app);

// Remote Config is browser-only
const remoteConfig = typeof window !== "undefined" ? getRemoteConfig(app) : null;

export { app, auth, db, storage, remoteConfig };

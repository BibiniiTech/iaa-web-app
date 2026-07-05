'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { useRouter, usePathname } from 'next/navigation';
import { signInWithEmailAndPassword, GoogleAuthProvider, signInWithPopup, sendPasswordResetEmail } from 'firebase/auth';
import { auth } from '@/lib/firebase';
import styles from './login.module.css';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const router = useRouter();
  const pathname = usePathname();

  const handleEmailLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !password) {
      setError('Please enter both email and password.');
      return;
    }
    setError(null);
    setMessage(null);
    setLoading(true);
    try {
      await signInWithEmailAndPassword(auth, email.trim(), password);
      if (pathname === '/login') {
        router.push('/');
      }
    } catch (err: any) {
      setError('Authentication failed. Please verify credentials.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleLogin = async () => {
    setError(null);
    setMessage(null);
    setLoading(true);
    try {
      const provider = new GoogleAuthProvider();
      await signInWithPopup(auth, provider);
      if (pathname === '/login') {
        router.push('/');
      }
    } catch (err: any) {
      setError(err.message || 'Google sign in failed');
    } finally {
      setLoading(false);
    }
  };

  const handleForgotPassword = async () => {
    if (!email) {
      setError('Please enter your email to reset your password.');
      return;
    }
    setError(null);
    setMessage(null);
    try {
      await sendPasswordResetEmail(auth, email.trim());
      setMessage('Reset link sent to your email.');
    } catch (err: any) {
      setError('Failed to send reset link. Verify your email.');
    }
  };

  return (
    <div className={styles.authContainer}>
      <header className={styles.topBar}>
        <h2 className={styles.topBarTitle}>Sign In</h2>
      </header>

      <main className={styles.mainContent}>
        <div className={styles.authCard}>
          <div className={styles.logoContainer}>
            <Image
              src="/new_logo.png"
              alt="App Logo"
              width={120}
              height={120}
              className={styles.logo}
              priority
            />
          </div>

          <h1 className={styles.authTitle}>Access Your Account</h1>
          <p className={styles.authSubtitle}>
            This app is designed for use by Internal Auditors of MDAs, SOEs, RCCs & MMDAs in Ghana under the supervision of the Internal Audit Agency
          </p>

          {error && <div className={styles.errorMessage}>{error}</div>}
          {message && <div className={styles.successMessage}>{message}</div>}

          <form className={styles.authForm} onSubmit={handleEmailLogin}>
            <div className={styles.inputGroup}>
              <div className={styles.inputWrapper}>
                <span className={styles.inputIcon}>
                  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="m22 2-7 20-4-9-9-4Z"/><path d="M22 2 11 13"/></svg>
                </span>
                <input
                  type="email"
                  id="email"
                  className={styles.inputField}
                  value={email}
                  onChange={(e) => { setEmail(e.target.value); setError(null); }}
                  placeholder="Email Address"
                  required
                />
              </div>
            </div>

            <div className={styles.inputGroup}>
              <div className={styles.inputWrapper}>
                <span className={styles.inputIcon}>
                  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect width="18" height="11" x="3" y="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                </span>
                <input
                  type={showPassword ? "text" : "password"}
                  id="password"
                  className={styles.inputField}
                  value={password}
                  onChange={(e) => { setPassword(e.target.value); setError(null); }}
                  placeholder="Password"
                  required
                />
                <button
                  type="button"
                  className={styles.visibilityToggle}
                  onClick={() => setShowPassword(!showPassword)}
                >
                  {showPassword ? (
                    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z"/><circle cx="12" cy="12" r="3"/><line x1="2" x2="22" y1="2" y2="22"/></svg>
                  ) : (
                    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z"/><circle cx="12" cy="12" r="3"/></svg>
                  )}
                </button>
              </div>
            </div>

            <button type="button" onClick={handleForgotPassword} className={styles.forgotPassword}>
              Forgot Password?
            </button>

            <button type="submit" className={styles.submitButton} disabled={loading}>
              {loading ? (
                <div className={styles.spinner}></div>
              ) : 'Sign In'}
            </button>
          </form>

          <button className={styles.googleButton} onClick={handleGoogleLogin} disabled={loading}>
            <img src="https://www.gstatic.com/firebasejs/ui/2.0.0/images/auth/google.svg" alt="Google" width="20" />
            <span>Continue with Google</span>
          </button>

          <div className={styles.divider}>
            <span>OR</span>
          </div>

          <div className={styles.signUpCard}>
            <p className={styles.signUpPrompt}>Click below to create an account</p>
            <Link href="/signup" className={styles.signUpButton}>
              Sign Up Here
            </Link>
          </div>
        </div>
      </main>
    </div>
  );
}

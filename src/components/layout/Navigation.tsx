'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { usePathname, useRouter } from 'next/navigation';
import styles from './Navigation.module.css';

import { auth, db } from '@/lib/firebase';
import { onAuthStateChanged, signOut, User } from 'firebase/auth';
import { doc, getDoc } from 'firebase/firestore';
import LoginPage from '@/app/login/page';
import Disclaimer from './Disclaimer';

type IconName =
  | 'home'
  | 'libraryBooks'
  | 'autoAwesome'
  | 'calculate'
  | 'send'
  | 'school'
  | 'leaderboard'
  | 'contactPage'
  | 'gavel'
  | 'adminPanelSettings'
  | 'logout'
  | 'person'
  | 'menu'
  | 'close';

type NavItem = {
  label: string;
  href: string;
  icon: IconName;
};

const NAV_ITEMS: NavItem[] = [
  { label: 'Home', href: '/', icon: 'home' },
  { label: 'Resources', href: '/resources', icon: 'libraryBooks' },
  { label: 'Findings & Criteria', href: '/findings', icon: 'autoAwesome' },
  { label: 'Tax Calculator', href: '/calculators', icon: 'calculate' },
  { label: 'Submissions', href: '/submissions', icon: 'send' },
  { label: 'Trainings', href: '/trainings', icon: 'school' },
  { label: 'PFM League', href: '/pfm-league', icon: 'leaderboard' },
  { label: 'Contact', href: '/contact', icon: 'contactPage' },
  { label: 'Profile', href: '/profile', icon: 'person' },
  { label: 'Disclaimer', href: '/disclaimer', icon: 'gavel' },
];

const ICON_PATHS: Record<IconName, string> = {
  home: 'M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z',
  libraryBooks: 'M20 6h-8.18C11.4 4.84 10.3 4 9 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm0 12H4V6h5c.55 0 1 .45 1 1v1h10v10zM6 10h8v2H6v-2zm0 3h8v2H6v-2z',
  autoAwesome: 'm19 9 1.25-2.75L23 5l-2.75-1.25L19 1l-1.25 2.75L15 5l2.75 1.25L19 9zm-7.5.5L9 4 6.5 9.5 1 12l5.5 2.5L9 20l2.5-5.5L17 12l-5.5-2.5zm7.5 5.5-1.25 2.75L15 19l2.75 1.25L19 23l1.25-2.75L23 19l-2.75-1.25L19 15z',
  calculate: 'M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM7 7h10v3H7V7zm2 10H7v-2h2v2zm0-4H7v-2h2v2zm4 4h-2v-2h2v2zm0-4h-2v-2h2v2zm4 4h-2v-6h2v6z',
  send: 'M2 21 23 12 2 3v7l15 2-15 2v7z',
  school: 'M12 3 1 9l11 6 9-4.91V17h2V9L12 3zm0 13.5L5 12.67v4L12 20l7-3.33v-4L12 16.5z',
  leaderboard: 'M16 11V3H8v8H2v10h20V11h-6zM4 19v-6h4v6H4zm10 0h-4V5h4v14zm6 0h-4v-6h4v6z',
  contactPage: 'M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zm-2 15H7v-1.5c0-1.66 3.33-2.5 5-2.5s5 .84 5 2.5V17h-5zm0-5c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm1-3V3.5L18.5 9H13z',
  gavel: 'm5.22 8.57 3.53 3.53 4.24-4.24L9.46 4.33a1.5 1.5 0 0 0-2.12 0L5.22 6.45a1.5 1.5 0 0 0 0 2.12zM16.54 11l-4.24 4.24 3.53 3.53a1.5 1.5 0 0 0 2.12 0l2.12-2.12a1.5 1.5 0 0 0 0-2.12L16.54 11zM10.5 15l-1.41-1.41L2.12 20.5a1 1 0 0 0 0 1.41l.71.71a1 1 0 0 0 1.41 0L10.5 15z',
  adminPanelSettings: 'M17 11c.34 0 .67.04 1 .1V5.27L10.5 2 3 5.27v4.91c0 4.54 3.2 8.79 7.5 9.82.55-.13 1.08-.32 1.59-.55A6.99 6.99 0 0 1 17 11zm0 2c-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4-1.79-4-4-4zm0 1.38c.62 0 1.12.5 1.12 1.12s-.5 1.12-1.12 1.12-1.12-.5-1.12-1.12.5-1.12 1.12-1.12zm0 5.24c-.93 0-1.74-.46-2.24-1.17.05-.74 1.5-1.15 2.24-1.15.75 0 2.19.41 2.24 1.15-.5.71-1.31 1.17-2.24 1.17z',
  logout: 'M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.59L17 17l5-5-5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z',
  person: 'M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z',
  menu: 'M3 6h18v2H3V6zm0 5h18v2H3v-2zm0 5h18v2H3v-2z',
  close: 'M18.3 5.71 12 12l6.3 6.29-1.41 1.41L10.59 13.41 4.29 19.71 2.88 18.3 9.17 12 2.88 5.71 4.29 4.29l6.3 6.3 6.29-6.3z',
};
;

function MaterialIcon({ name }: { name: IconName }) {
  return (
    <svg className={styles.icon} viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path d={ICON_PATHS[name]} />
    </svg>
  );
}

function NavLinks({
  isAdmin,
  pathname,
  closeDrawer,
  showPfmLeague,
  showDisclaimer,
}: {
  isAdmin: boolean;
  pathname: string;
  closeDrawer: () => void;
  showPfmLeague: boolean;
  showDisclaimer: boolean;
}) {
  return (
    <div className={styles.navLinks}>
      {NAV_ITEMS.map((item) => {
        if (item.href === '/pfm-league' && !showPfmLeague) return null;
        if (item.href === '/disclaimer' && !showDisclaimer) return null;
        return (
          <Link
            key={item.href}
            href={item.href}
            className={`${styles.navLink} ${pathname === item.href ? styles.activeLink : ''}`}
            onClick={closeDrawer}
          >
            <MaterialIcon name={item.icon} />
            <span>{item.label}</span>
          </Link>
        );
      })}
      {isAdmin && (
        <Link
          href="/admin"
          className={`${styles.navLink} ${pathname === '/admin' ? styles.activeLink : ''}`}
          onClick={closeDrawer}
        >
          <MaterialIcon name="adminPanelSettings" />
          <span>Admin Dashboard</span>
        </Link>
      )}
    </div>
  );
}

export default function Navigation({ children }: { children: React.ReactNode }) {
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [isAdmin, setIsAdmin] = useState(false);
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [showDisclaimer, setShowDisclaimer] = useState(false);
  const [disclaimerAccepted, setDisclaimerAccepted] = useState(true);
  const [showPfmLeague, setShowPfmLeague] = useState(true);
  const pathname = usePathname();
  const router = useRouter();

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (firebaseUser) => {
      setUser(firebaseUser);
      if (firebaseUser) {
        const [userDoc, disclaimerSnap, pfmSnap] = await Promise.all([
          getDoc(doc(db, 'users', firebaseUser.uid)),
          getDoc(doc(db, 'config', 'show_disclaimer')),
          getDoc(doc(db, 'config', 'show_pfm_league')),
        ]);

        setIsAdmin(userDoc.exists() && userDoc.data().role === 'admin');

        if (disclaimerSnap.exists() && disclaimerSnap.data().show) {
          setShowDisclaimer(true);
          const accepted = localStorage.getItem('disclaimer_accepted');
          setDisclaimerAccepted(!!accepted);
        } else {
          setShowDisclaimer(false);
          setDisclaimerAccepted(true);
        }

        if (pfmSnap.exists()) {
          setShowPfmLeague(pfmSnap.data().show ?? true);
        }
      } else {
        setIsAdmin(false);
        setShowDisclaimer(false);
        setDisclaimerAccepted(true);
        setShowPfmLeague(true);
      }
      setLoading(false);
    });
    return () => unsubscribe();
  }, []);

  useEffect(() => {
    if (loading) return;

    if (pathname === '/disclaimer' && !showDisclaimer) {
      router.push('/');
    }
    if (pathname === '/pfm-league' && !showPfmLeague) {
      router.push('/');
    }
  }, [pathname, showDisclaimer, showPfmLeague, loading, router]);

  const handleAcceptDisclaimer = () => {
    localStorage.setItem('disclaimer_accepted', 'true');
    setDisclaimerAccepted(true);
  };

  const handleSignOut = async () => {
    await signOut(auth);
    router.push('/login');
  };

  const closeDrawer = () => setIsDrawerOpen(false);
  const toggleDrawer = () => setIsDrawerOpen((open) => !open);

  const isAuthPage = pathname === '/login' || pathname === '/signup';

  if (loading) {
    return (
      <div className={styles.loading}>
        <Image src="/iaa_logo.png" alt="IAA Logo" width={180} height={60} priority />
      </div>
    );
  }

  if (!user) {
    return isAuthPage ? <main className={styles.authOnlyContent}>{children}</main> : <LoginPage />;
  }

  return (
    <div className={`${styles.navWrapper} ${isDrawerOpen ? styles.drawerOpen : ''}`}>
      {showDisclaimer && !disclaimerAccepted && !isAuthPage && (
        <Disclaimer onAccept={handleAcceptDisclaimer} />
      )}
      <aside className={styles.sidebar}>
        <div className={styles.logoArea}>
          <Image src="/iaa_logo.png" alt="IAA Logo" width={180} height={60} className={styles.logoImage} style={{ width: 'auto', height: 'auto' }} priority />
        </div>
        <NavLinks
          isAdmin={isAdmin}
          pathname={pathname}
          closeDrawer={closeDrawer}
          showPfmLeague={showPfmLeague}
          showDisclaimer={showDisclaimer}
        />
        <div className={styles.footer}>
          <button className={`${styles.navLink} ${styles.signOutButton}`} onClick={handleSignOut} type="button">
            <MaterialIcon name="logout" />
            <span>Sign Out</span>
          </button>
        </div>
      </aside>

      <header className={styles.mobileHeader}>
        <div className={styles.mobileLogoArea}>
          <Image src="/iaa_logo.png" alt="IAA Logo" width={142} height={48} className={styles.logoImage} style={{ width: 'auto', height: 'auto' }} priority />
        </div>
        <button className={styles.menuButton} onClick={toggleDrawer} type="button" aria-label="Open navigation menu">
          <MaterialIcon name="menu" />
        </button>
      </header>

      <div className={styles.drawerOverlay} onClick={toggleDrawer}></div>
      <div className={styles.mobileDrawer}>
        <button className={styles.closeButton} onClick={toggleDrawer} type="button" aria-label="Close navigation menu">
          <MaterialIcon name="close" />
        </button>
        <NavLinks
          isAdmin={isAdmin}
          pathname={pathname}
          closeDrawer={closeDrawer}
          showPfmLeague={showPfmLeague}
          showDisclaimer={showDisclaimer}
        />
        <div className={styles.mobileFooter}>
          <button className={`${styles.navLink} ${styles.signOutButton}`} onClick={handleSignOut} type="button">
            <MaterialIcon name="logout" />
            <span>Sign Out</span>
          </button>
        </div>
      </div>

      <main className={styles.mainContent}>
        <div className={styles.pageBody}>
          {children}
        </div>
        {!isAuthPage && (
          <footer className={styles.globalFooter}>
            <p>© 2026 BibiniiTech Ghana. All rights reserved.</p>
          </footer>
        )}
        {!isAuthPage && (
          <div className={styles.globalFooterBg}>
            <Image
              src="/bg_footer.png"
              alt=""
              fill
              style={{ objectFit: 'cover', objectPosition: 'bottom', opacity: 0.15 }}
            />
          </div>
        )}
      </main>
    </div>
  );
}

import { NextResponse } from 'next/server';

type BrevoPerson = {
  name?: string;
  email: string;
};

type WebAttachment = {
  url: string;
  name: string;
};

type BrevoResponse = {
  message?: string;
  messageId?: string;
};

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

// Sliding-window rate limit per authenticated user / IP to prevent accidental
// or malicious exhaustion of the Brevo 300 emails/day free tier under heavy load.
const RATE_LIMIT_WINDOW_MS = 60 * 1000; // 1 minute
const MAX_EMAILS_PER_WINDOW = 5;
const callerRateLimits = new Map<string, number[]>();

function isRateLimited(callerKey: string): boolean {
  const now = Date.now();
  const recent = (callerRateLimits.get(callerKey) || []).filter(
    (ts) => now - ts < RATE_LIMIT_WINDOW_MS
  );
  if (recent.length >= MAX_EMAILS_PER_WINDOW) {
    callerRateLimits.set(callerKey, recent);
    return true;
  }
  recent.push(now);
  callerRateLimits.set(callerKey, recent);
  return false;
}

function normalizePeople(value: unknown): BrevoPerson[] {
  if (!Array.isArray(value)) return [];

  return value
    .map((person) => {
      if (typeof person === 'string') {
        const email = person.trim().toLowerCase();
        return EMAIL_REGEX.test(email) ? { email } : null;
      }

      if (person && typeof person === 'object' && 'email' in person) {
        const email = String((person as { email?: unknown }).email || '').trim().toLowerCase();
        const name = String((person as { name?: unknown }).name || '').trim();
        if (!EMAIL_REGEX.test(email)) return null;
        return name ? { name, email } : { email };
      }

      return null;
    })
    .filter((person): person is BrevoPerson => person !== null);
}

function normalizeAttachments(value: unknown): WebAttachment[] | undefined {
  if (!Array.isArray(value)) return undefined;

  const attachments = value
    .map((attachment) => {
      if (!attachment || typeof attachment !== 'object') return null;
      const url = String((attachment as { url?: unknown }).url || '').trim();
      const name = String((attachment as { name?: unknown }).name || '').trim();
      if (!url || !name) return null;
      return { url, name };
    })
    .filter((attachment): attachment is WebAttachment => attachment !== null);

  return attachments.length > 0 ? attachments : undefined;
}

async function verifyFirebaseAuthToken(authHeader: string | null): Promise<string | null> {
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return null;
  }
  const idToken = authHeader.substring(7).trim();
  if (!idToken) return null;

  const apiKey = process.env.NEXT_PUBLIC_FIREBASE_API_KEY;
  if (!apiKey) {
    // If no Firebase API key is configured on server, cannot verify
    return null;
  }

  try {
    const res = await fetch(
      `https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=${apiKey}`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ idToken }),
      }
    );
    if (!res.ok) {
      return null;
    }
    const data = (await res.json()) as { users?: { localId?: string }[] };
    if (Array.isArray(data.users) && data.users.length > 0) {
      return data.users[0]?.localId || 'authenticated-user';
    }
    return null;
  } catch (err) {
    console.error('Firebase token verification failed:', err);
    return null;
  }
}

export async function POST(request: Request) {
  try {
    // 1. Authenticate caller
    const authHeader = request.headers.get('authorization');
    const callerUid = await verifyFirebaseAuthToken(authHeader);
    if (!callerUid) {
      return NextResponse.json(
        { error: 'Unauthorized: Valid authentication token required.' },
        { status: 401 }
      );
    }

    // 2. Enforce per-caller rate limit to protect Brevo daily quota
    const forwardedFor = request.headers.get('x-forwarded-for')?.split(',')[0]?.trim() || 'unknown-ip';
    const rateKey = `${callerUid}:${forwardedFor}`;
    if (isRateLimited(rateKey)) {
      return NextResponse.json(
        { error: 'Too many email requests. Please wait a minute before submitting again.' },
        { status: 429 }
      );
    }

    const body = await request.json();
    const rawTo = normalizePeople(body.to);
    const rawCc = normalizePeople(body.cc);
    const rawBcc = normalizePeople(body.bcc);

    const seenEmails = new Set<string>();
    const to = rawTo.filter((p) => {
      if (seenEmails.has(p.email)) return false;
      seenEmails.add(p.email);
      return true;
    });
    const cc = rawCc.filter((p) => {
      if (seenEmails.has(p.email)) return false;
      seenEmails.add(p.email);
      return true;
    });
    const bcc = rawBcc.filter((p) => {
      if (seenEmails.has(p.email)) return false;
      seenEmails.add(p.email);
      return true;
    });
    const attachments = normalizeAttachments(body.attachments);
    const subject = String(body.subject || '').trim();
    const htmlContent = String(body.htmlContent || '').trim();

    const apiKey = process.env.BREVO_API_KEY;
    if (!apiKey) {
      return NextResponse.json({ error: 'Brevo API Key not configured on server' }, { status: 500 });
    }

    if (to.length === 0) {
      return NextResponse.json({ error: "No valid 'TO' recipients provided" }, { status: 400 });
    }

    if (!subject || !htmlContent) {
      return NextResponse.json({ error: 'Subject and HTML content are required' }, { status: 400 });
    }

    const response = await fetch('https://api.brevo.com/v3/smtp/email', {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        'api-key': apiKey,
      },
      body: JSON.stringify({
        sender: {
          name: process.env.BREVO_SENDER_NAME || 'IAA App',
          email: process.env.BREVO_SENDER_EMAIL || 'bibiniitech@gmail.com',
        },
        to,
        cc: cc.length > 0 ? cc : undefined,
        bcc: bcc.length > 0 ? bcc : undefined,
        subject,
        htmlContent,
        attachment: attachments,
      }),
    });

    const responseText = await response.text();
    const data = responseText ? tryParseJson(responseText) : {};

    if (!response.ok) {
      return NextResponse.json(
        {
          error: data.message || 'Failed to send email',
          details: data,
        },
        { status: response.status }
      );
    }

    return NextResponse.json({ success: true, messageId: data.messageId });
  } catch (error: unknown) {
    console.error('Email API Error:', error);
    const message = error instanceof Error ? error.message : 'Internal Server Error';
    return NextResponse.json({ error: message }, { status: 500 });
  }
}

function tryParseJson(value: string): BrevoResponse {
  try {
    return JSON.parse(value);
  } catch {
    return {};
  }
}

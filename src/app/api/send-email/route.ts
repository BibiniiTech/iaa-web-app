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

async function verifyFirebaseAuthToken(authHeader: string | null): Promise<boolean> {
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return false;
  }
  const idToken = authHeader.substring(7).trim();
  if (!idToken) return false;

  const apiKey = process.env.NEXT_PUBLIC_FIREBASE_API_KEY;
  if (!apiKey) {
    // If no Firebase API key is configured on server, cannot verify
    return false;
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
      return false;
    }
    const data = (await res.json()) as { users?: unknown[] };
    return Array.isArray(data.users) && data.users.length > 0;
  } catch (err) {
    console.error('Firebase token verification failed:', err);
    return false;
  }
}

export async function POST(request: Request) {
  try {
    // 1. Authenticate caller
    const authHeader = request.headers.get('authorization');
    const isAuthed = await verifyFirebaseAuthToken(authHeader);
    if (!isAuthed) {
      return NextResponse.json(
        { error: 'Unauthorized: Valid authentication token required.' },
        { status: 401 }
      );
    }

    const body = await request.json();
    const to = normalizePeople(body.to);
    const cc = normalizePeople(body.cc);
    const bcc = normalizePeople(body.bcc);
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

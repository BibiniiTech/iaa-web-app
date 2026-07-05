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

function normalizePeople(value: unknown): BrevoPerson[] {
  if (!Array.isArray(value)) return [];

  return value
    .map((person) => {
      if (typeof person === 'string') {
        return { email: person.trim() };
      }

      if (person && typeof person === 'object' && 'email' in person) {
        const email = String((person as { email?: unknown }).email || '').trim();
        const name = String((person as { name?: unknown }).name || '').trim();
        return name ? { name, email } : { email };
      }

      return { email: '' };
    })
    .filter((person) => person.email);
}

function normalizeAttachments(value: unknown): WebAttachment[] | undefined {
  if (!Array.isArray(value)) return undefined;

  const attachments = value
    .map((attachment) => {
      if (!attachment || typeof attachment !== 'object') return null;
      const url = String((attachment as { url?: unknown }).url || '').trim();
      const name = String((attachment as { name?: unknown }).name || '').trim();
      return url && name ? { url, name } : null;
    })
    .filter((attachment): attachment is WebAttachment => attachment !== null);

  return attachments.length > 0 ? attachments : undefined;
}

export async function POST(request: Request) {
  try {
    const body = await request.json();
    const to = normalizePeople(body.to);
    const cc = normalizePeople(body.cc);
    const bcc = normalizePeople(body.bcc);
    const attachments = normalizeAttachments(body.attachments);
    const subject = String(body.subject || '');
    const htmlContent = String(body.htmlContent || '');

    const apiKey = process.env.BREVO_API_KEY;
    if (!apiKey) {
      return NextResponse.json({ error: 'Brevo API Key not configured' }, { status: 500 });
    }

    if (to.length === 0) {
      return NextResponse.json({ error: "No valid 'TO' recipients provided" }, { status: 400 });
    }

    const response = await fetch('https://api.brevo.com/v3/smtp/email', {
      method: 'POST',
      headers: {
        'Accept': 'application/json',
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
      return NextResponse.json({ error: data.message || 'Failed to send email' }, { status: response.status });
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

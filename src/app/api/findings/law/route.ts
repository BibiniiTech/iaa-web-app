import { NextResponse } from 'next/server';
import lawDetails from '@/data/law_details.json';

async function verifyFirebaseAuthToken(authHeader: string | null): Promise<string | null> {
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return null;
  }
  const idToken = authHeader.substring(7).trim();
  if (!idToken) return null;

  const apiKey = process.env.NEXT_PUBLIC_FIREBASE_API_KEY;
  if (!apiKey) return null;

  try {
    const res = await fetch(
      `https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=${apiKey}`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ idToken }),
      }
    );
    if (!res.ok) return null;
    const data = (await res.json()) as { users?: { localId?: string }[] };
    if (Array.isArray(data.users) && data.users.length > 0) {
      return data.users[0]?.localId || 'authenticated-user';
    }
    return null;
  } catch (err) {
    console.error('Firebase token verification error in law route:', err);
    return null;
  }
}

export async function GET(request: Request) {
  try {
    const authHeader = request.headers.get('authorization');
    const callerUid = await verifyFirebaseAuthToken(authHeader);
    if (!callerUid) {
      return NextResponse.json(
        { error: 'Unauthorized: Authentication required.' },
        { status: 401 }
      );
    }

    const { searchParams } = new URL(request.url);
    const key = (searchParams.get('key') || '').trim();

    if (!key) {
      return NextResponse.json({ error: 'Missing law key parameter' }, { status: 400 });
    }

    const dict = lawDetails as Record<string, string>;
    const text = dict[key] || null;

    return NextResponse.json({ key, text });
  } catch (err: unknown) {
    console.error('Law API Error:', err);
    return NextResponse.json({ error: 'Internal Server Error' }, { status: 500 });
  }
}

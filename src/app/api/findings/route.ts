import { NextResponse } from 'next/server';
import findingsData from '@/data/audit_findings.json';
import lawDetails from '@/data/law_details.json';

export interface Finding {
  id: string;
  category: string;
  observation: string;
  criteria: string[];
  applicableTo: string;
}

const STOP_WORDS = new Set([
  'in', 'to', 'of', 'on', 'and', 'the', 'for', 'a', 'an', 'is', 'are', 'that',
  'this', 'which', 'with', 'at', 'by', 'from', 'or', 'as'
]);

const SYNONYMS: Record<string, string> = {
  vat: 'value added tax',
  pfm: 'public financial management',
  pfmr: 'public financial management regulations li 2378',
  ppa: 'public procurement act 663',
  iaa: 'internal audit agency',
  gifmis: 'ghana integrated financial management information system',
  igf: 'internally generated funds',
  gcr: 'general counterfoil receipt',
  wht: 'withholding tax',
  paye: 'pay as you earn',
  'act 921': 'public financial management act',
  'li 2378': 'public financial management regulations',
  cash: 'money funds currency cheque bank float imprest',
  money: 'cash funds currency cheque bank float imprest',
  funds: 'cash money currency bank capital float imprest',
  fund: 'cash money currency bank capital float imprest',
  payment: 'disbursement voucher cheque pv',
  disbursement: 'payment voucher cheque pv',
  voucher: 'pv payment receipt invoice document',
  pv: 'voucher payment disbursement',
  invoice: 'receipt bill voucher document',
  receipt: 'gcr invoice voucher bill payment',
  imprest: 'petty cash float advance funds',
  advance: 'imprest float petty cash funds',
  revenue: 'igf income tax collection receipt collection',
  income: 'revenue igf collection',
  stores: 'inventory stock items goods warehouse',
  stock: 'stores inventory items goods warehouse',
  inventory: 'stores stock items goods warehouse',
  goods: 'items stock inventory stores asset asset',
  purchases: 'procurement goods contract tender ppa',
  contract: 'agreement procurement project tender ppa',
  procurement: 'purchases contract tender ppa buy',
  tax: 'gra wht paye levy duty tariff statutory',
  salary: 'payroll wage paye ssnit compensation staff employee',
  payroll: 'salary wage paye ssnit compensation staff employee',
  wage: 'salary payroll ssnit compensation staff employee',
  staff: 'employee human resource personnel hr worker officers',
  employee: 'staff human resource personnel hr worker officer',
  personnel: 'staff employee human resource hr worker officer',
  vehicle: 'transport car automobile fuel logbook transport',
  car: 'vehicle transport fuel logbook transport',
  transport: 'vehicle car fuel logbook transit',
  fuel: 'petrol diesel vehicle car transport coupon logbook',
  fictitious: 'fake forged fabricated false dummy simulated phantom non-existent',
  fake: 'fictitious forged fabricated false dummy simulated',
  forged: 'fictitious fake fabricated false copy altered simulated',
  suspicious: 'dubious questionable fishy anomalous irregular unusual',
  irregular: 'suspicious dubious questionable fishy anomalous unusual exception non-compliant',
  fraudulent: 'dishonest corrupt illegal misappropriated unauthorized unapproved stolen missing theft',
  corrupt: 'fraudulent dishonest illegal misappropriated bribe kickback',
  illegal: 'unlawful non-compliant unauthorized unapproved prohibited fraudulent corrupt',
  unauthorized: 'unapproved non-compliant illegal unlawful disallowed',
  missing: 'lost stolen short count shortage unaccounted theft',
  stolen: 'missing lost shortage misappropriated theft',
};

const levenshteinDistance = (s1: string, s2: string): number => {
  const d: number[][] = Array.from({ length: s1.length + 1 }, () => Array(s2.length + 1).fill(0));
  for (let i = 0; i <= s1.length; i++) d[i][0] = i;
  for (let j = 0; j <= s2.length; j++) d[0][j] = j;
  for (let i = 1; i <= s1.length; i++) {
    for (let j = 1; j <= s2.length; j++) {
      const cost = s1[i - 1] === s2[j - 1] ? 0 : 1;
      d[i][j] = Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost);
    }
  }
  return d[s1.length][s2.length];
};

const isFuzzyMatch = (word: string, target: string): boolean => {
  if (word.length < 4 || target.length < 4) return word === target;
  return levenshteinDistance(word, target) <= 1;
};

const expandQueryWithSynonyms = (query: string): string => {
  let expanded = query;
  const words = query.toLowerCase().split(/\W+/).filter(Boolean);
  Object.entries(SYNONYMS).forEach(([abbr, full]) => {
    const isMatch = abbr.includes(' ') ? query.toLowerCase().includes(abbr) : words.includes(abbr);
    if (isMatch) expanded += ` ${full}`;
  });
  return expanded;
};

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
    console.error('Firebase token verification error:', err);
    return null;
  }
}

export async function GET(request: Request) {
  try {
    const authHeader = request.headers.get('authorization');
    const callerUid = await verifyFirebaseAuthToken(authHeader);
    if (!callerUid) {
      return NextResponse.json(
        { error: 'Unauthorized: Authentication required to access audit findings database.' },
        { status: 401 }
      );
    }

    const { searchParams } = new URL(request.url);
    const query = (searchParams.get('q') || '').trim();
    const category = searchParams.get('category');
    const page = Math.max(1, parseInt(searchParams.get('page') || '1', 10));
    const limit = Math.min(100, Math.max(5, parseInt(searchParams.get('limit') || '30', 10)));

    let list = findingsData as Finding[];

    // 1. Filter by category
    if (category && category !== 'all' && category !== 'All') {
      list = list.filter((f) => f.category === category);
    }

    // 2. Filter by search query
    if (query) {
      const expandedQuery = expandQueryWithSynonyms(query);
      const queryWords = expandedQuery
        .toLowerCase()
        .split(/\W+/)
        .filter((w) => w.length > 0 && !STOP_WORDS.has(w));

      if (queryWords.length > 0) {
        list = list.filter((f) => {
          const obs = f.observation.toLowerCase();
          const crit = f.criteria.join(' ').toLowerCase();
          const cat = f.category.toLowerCase();
          const targetText = `${obs} ${crit} ${cat}`;
          const targetWords = targetText.split(/\W+/).filter(Boolean);

          return queryWords.every((qw) => {
            if (targetText.includes(qw)) return true;
            return targetWords.some((tw) => isFuzzyMatch(qw, tw));
          });
        });
      }
    }

    const total = list.length;
    const totalPages = Math.ceil(total / limit);
    const offset = (page - 1) * limit;
    const paginatedFindings = list.slice(offset, offset + limit);

    return NextResponse.json({
      findings: paginatedFindings,
      total,
      page,
      totalPages,
      limit,
    });
  } catch (err: unknown) {
    console.error('Findings API Error:', err);
    return NextResponse.json({ error: 'Internal Server Error' }, { status: 500 });
  }
}

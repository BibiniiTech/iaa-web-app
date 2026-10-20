import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

export function middleware(request: NextRequest) {
  const pathname = request.nextUrl.pathname;

  // Protect Admin route from unauthenticated/unauthorized access at the Edge
  if (pathname.startsWith('/admin')) {
    const adminCookie = request.cookies.get('iaa_admin')?.value;

    // If no verified admin cookie is present, redirect to home page
    if (adminCookie !== '1') {
      const redirectUrl = new URL('/', request.url);
      return NextResponse.redirect(redirectUrl);
    }
  }

  return NextResponse.next();
}

export const config = {
  matcher: ['/admin/:path*'],
};

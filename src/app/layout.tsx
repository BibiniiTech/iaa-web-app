import type { Metadata, Viewport } from "next";
import "./globals.css";
import Navigation from "@/components/layout/Navigation";

export const metadata: Metadata = {
  title: "IAA App",
  description: "Internal Audit Agency App",
  icons: {
    icon: [
      { url: "/favicon.ico", sizes: "any" },
      { url: "/icon.png", type: "image/png" },
    ],
    shortcut: "/favicon.ico",
    apple: "/apple-icon.png",
  },
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  viewportFit: "cover",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body>
        <Navigation>
          {children}
        </Navigation>
      </body>
    </html>
  );
}

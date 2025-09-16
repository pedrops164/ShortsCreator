import type { Metadata } from 'next';
import './styles/globals.css';
import { ThemeProvider } from '@/components/layout/ThemeProvider';
import SessionProvider from "@/components/layout/SessionProvider";
import SessionChecker from '@/components/SessionChecker';
import Script from 'next/script';

export const metadata: Metadata = {
  // Updated Title and Description
  title: {
    template: '%s | Mad Shorts',
    default: 'Mad Shorts | AI Video Generator for Viral Content',
  },
  description: 'Automate your short-form content. Generate viral, AI-narrated videos from Reddit stories and more. Features background gameplay and character voices like Morty Smith and Peter Griffin. Sign in to start creating for free!',
};

/**
 * This is the root layout for the entire application.
 * It wraps every page with the shared UI from the <Layout> component.
 */
export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body>
        <SessionProvider>
          <SessionChecker />
          <ThemeProvider attribute="class" defaultTheme="dark" enableSystem={false} disableTransitionOnChange={false}>
            <div className="flex h-screen bg-background">
              {children}
            </div>
          </ThemeProvider>
        </SessionProvider>
        {/* Google Analytics Script for all pages */}
        <Script
          strategy="afterInteractive"
          src={`https://www.googletagmanager.com/gtag/js?id=${process.env.NEXT_PUBLIC_GA_MEASUREMENT_ID}`}
        />
        <Script id="google-analytics" strategy="afterInteractive">
          {`
            window.dataLayer = window.dataLayer || [];
            function gtag(){dataLayer.push(arguments);}
            gtag('js', new Date());
            gtag('config', '${process.env.NEXT_PUBLIC_GA_MEASUREMENT_ID}', {
              page_path: window.location.pathname,
            });
          `}
        </Script>
      </body>
    </html>
  );
}
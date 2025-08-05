import type { Metadata } from 'next';
import './styles/globals.css';
import { ThemeProvider } from '@/components/layout/ThemeProvider';
import SessionProvider from "@/components/layout/SessionProvider";
import SessionChecker from '@/components/SessionChecker';

export const metadata: Metadata = {
  title: 'Shorts Creator',
  description: 'Automate your short form content',
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
      </body>
    </html>
  );
}
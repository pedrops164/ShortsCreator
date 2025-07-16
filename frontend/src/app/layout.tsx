import type { Metadata } from 'next';
import './styles/globals.css';
import { ThemeProvider } from './providers';
import NavBar from "@/components/layout/NavBar";
import SessionProvider from "@/components/layout/SessionProvider";

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
          <ThemeProvider>
            <NavBar>
              {/* The NavBar component will render the sidebar and main content */}
              {children}
            </NavBar>
          </ThemeProvider>
        </SessionProvider>
      </body>
    </html>
  );
}
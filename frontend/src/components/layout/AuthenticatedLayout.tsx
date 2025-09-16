'use client';

import { useSession, signOut } from 'next-auth/react';
import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { NotificationProvider } from '@/context/NotificationContext';
import { BalanceProvider } from '@/context/BalanceContext';

export default function AuthenticatedLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();

  // This hook is the key.
  // - `required: true`: It will automatically redirect to the login page if there's no session.
  // - `onUnauthenticated`: A callback function to run upon redirection.
  const { data: session, status } = useSession({
    required: true,
    onUnauthenticated() {
      // Redirect to your login page
      router.push('/');
    },
  });

  // This effect handles the case where a token refresh fails.
  useEffect(() => {
    if (session?.error === 'RefreshAccessTokenError') {
      signOut(); // Force sign out
    }
  }, [session]);

  // While the session is being verified, show a loading state.
  // This prevents any children from rendering and trying to fetch data.
  if (status === 'loading') {
    return <div>Loading session...</div>;
  }
  
  // Once authenticated, render the full layout with the NotificationProvider.
  return (
    <NotificationProvider>
      <BalanceProvider>
        {children}
      </BalanceProvider>
    </NotificationProvider>
  );
}
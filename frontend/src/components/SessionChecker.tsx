'use client';

import { useSession, signOut } from 'next-auth/react';
import { useEffect } from 'react';

export default function SessionChecker() {
  const { data: session } = useSession();

  useEffect(() => {
    // Check if the session exists and has the specific error
    if (session?.error === 'RefreshAccessTokenError') {
      // If the error is present, force the user to sign out
      console.error('Session expired, signing out...');
      signOut(); // Redirect to login page after sign out
    }
  }, [session]); // Rerun this effect when the session object changes

  // This component does not render anything
  return null;
}
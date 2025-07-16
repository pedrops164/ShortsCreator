'use client';

import { useSession, signIn, signOut } from 'next-auth/react';
import apiClient from '@/lib/apiClient';
import { useEffect, useState } from 'react';

// A simple type for the draft object
type Draft = {
  id: string;
  templateId: string;
  templateParams: {
    postTitle: string;
    postDescription: string;
  };
};

export default function DashboardPage() {
  const { data: session, status } = useSession();
  const [drafts, setDrafts] = useState<Draft[]>([]);

  useEffect(() => {
    const fetchDrafts = async () => {
      if (status === 'authenticated') {
        try {
          // This request will go to http://localhost:8081/api/v1/content/drafts
          // with the Authorization header already attached.
          const response = await apiClient.get('/content/drafts');
          console.log('Drafts fetched:', response.data);
          setDrafts(response.data);
        } catch (error) {
          console.error('Failed to fetch drafts:', error);
        }
      }
    };
    fetchDrafts();
  }, [status]);


  if (status === 'loading') {
    return (
      <div className="bg-background text-foreground min-h-screen flex items-center justify-center">
        <p>Loading...</p>
      </div>
    );
  }

  if (status === 'unauthenticated') {
    return (
      // Use flexbox to center the content
      <div className="bg-background text-foreground min-h-screen flex flex-col items-center justify-center gap-4">
        <h1 className="text-3xl font-bold">Access Denied</h1>
        {/* Themed button */}
        <button
          onClick={() => signIn('keycloak')}
          className="bg-primary text-white font-bold py-2 px-4 rounded-lg hover:bg-primary/90 transition-colors"
        >
          Sign In
        </button>
      </div>
    );
  }

  return (
    // Set the base background and text color for the page
    <div className="bg-background text-foreground min-h-screen p-4 sm:p-8">
      <div className="max-w-4xl mx-auto">
        <div className="flex justify-between items-center mb-8">
          {/* Use the primary color for the main heading */}
          <h1 className="text-3xl font-bold text-primary">
            Welcome, {session.user?.name}
          </h1>
          {/* Themed button for signing out */}
          <button
            onClick={() => signOut()}
            className="border border-foreground/30 font-semibold py-2 px-4 rounded-lg hover:bg-foreground/10 transition-colors"
          >
            Sign Out
          </button>
        </div>

        <h2 className="text-2xl font-semibold border-b border-accent pb-2 mb-4">
          Your Drafts
        </h2>

        {drafts.length > 0 ? (
          <ul className="space-y-4">
            {drafts.map((draft) => (
              // Use the accent color for borders to create 'cards'
              <li key={draft.id} className="border border-accent rounded-lg p-4 bg-background/50">
                <h3 className="text-xl font-bold">{draft.templateParams.postTitle}</h3>
                {/* Use a slightly muted text color for less important text */}
                <p className="text-foreground/80 mt-1">{draft.templateParams.postDescription}</p>
              </li>
            ))}
          </ul>
        ) : (
          <p className="text-foreground/70">You have no drafts.</p>
        )}
      </div>
    </div>
  );
}

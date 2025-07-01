'use client';

import { useSession, signIn, signOut } from 'next-auth/react';
import apiClient from '@/lib/apiClient';
import { useEffect, useState } from 'react';

export default function DashboardPage() {
  const { data: session, status } = useSession();
  const [drafts, setDrafts] = useState([]);

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
    return <div>Loading...</div>;
  }

  if (status === 'unauthenticated') {
    return (
      <div>
        <h1>Access Denied</h1>
        <button onClick={() => signIn('keycloak')}>Sign In</button>
      </div>
    );
  }

  return (
    <div>
      <h1>Welcome, {session.user?.name}</h1>
      <button onClick={() => signOut()}>Sign Out</button>
      <h2>Your Drafts:</h2>
      {/* Render your drafts here */}
      <ul>
        {drafts.map((draft) => (
          <li key={draft.id}>
            <h3>{draft.templateId}</h3>
            <p>{draft.templateParams['postTitle']}</p>
            <p>{draft.templateParams.postDescription}</p>
          </li>
        ))}
      </ul>
    </div>
  );
}

// src/app/(auth)/layout.tsx

import { Metadata } from 'next';
import React from 'react';

export const metadata: Metadata = {
  title: 'Create Viral Shorts From Reddit Stories | Mad Shorts',
  description: 'Generate viral, AI-narrated videos from Reddit stories and more. Features background gameplay and character voices like Morty Smith and Peter Griffin. Sign in to start creating for free!',
};

// This layout component will wrap your page
export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <>{children}</>;
}
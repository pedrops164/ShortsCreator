'use client';

import React, { Suspense, useState } from 'react';
import { signIn } from 'next-auth/react';
import { useSearchParams } from 'next/navigation';
import Image from 'next/image';
import { Loader2 } from 'lucide-react'; // Import Loader2 from lucide-react

const GoogleSignInButton = () => {
  const searchParams = useSearchParams();
  const callbackUrl = searchParams.get('callbackUrl') || '/create';
  
  // Add a loading state
  const [isLoading, setIsLoading] = useState(false);

  const handleSignIn = async () => {
    // Set loading to true when the sign-in process starts
    setIsLoading(true);
    // We call the 'keycloak' provider, pass the callbackUrl as the second argument, and pass an object with 'kc_idp_hint: google' as the third argument.
    signIn('keycloak', { callbackUrl }, { kc_idp_hint: 'google' });
  };

  return (
    <button
      onClick={handleSignIn}
      disabled={isLoading}
      className="relative bg-white text-gray-700 font-semibold py-2 px-4 border border-gray-300 rounded-lg shadow-sm hover:bg-gray-50 transition-colors flex items-center justify-center gap-3 w-64 disabled:opacity-70 disabled:cursor-not-allowed"
    >
      {isLoading ? (
        <>
          {/* Invisible placeholder to maintain the button's original size */}
          <span className="invisible flex items-center gap-3">
            <Image src="/google-logo.svg" alt="" width={20} height={20} />
            Sign in with Google
          </span>

          {/* Absolutely positioned spinner, centered on top and larger */}
          <div className="absolute inset-0 flex items-center justify-center">
            <Loader2 className="h-8 w-8 animate-spin" />
          </div>
        </>
      ) : (
        <>
          <Image src="/google-logo.png" alt="Google logo" width={20} height={20} />
          <span>Sign in with Google</span>
        </>
      )}
    </button>
  );
};

// --- Array of example video sources ---
const videoSources = [
  '/videos/test_reddit_story.mp4',
  '/videos/test_reddit_story.mp4',
  '/videos/test_reddit_story.mp4',
  '/videos/test_reddit_story.mp4',
];

export default function LoginPage() {
  return (
    <div className="relative min-h-screen w-full flex items-center justify-center bg-gray-900 overflow-hidden">
      {/* --- Animated Video Background --- */}
      <div className="absolute top-0 left-0 w-full h-full">
        {/* The container that scrolls, using the animation defined in globals.css */}
        <div className="absolute top-1/2 left-0 flex -translate-y-1/2 animate-marquee">
          {/* We render the list of videos twice to create the seamless loop effect */}
          {[...videoSources, ...videoSources].map((src, index) => (
            <div key={index} className="flex-shrink-0 w-96 mx-4">
              <div className="h-[640px] bg-gray-800 rounded-2xl overflow-hidden shadow-2xl">
                <video
                  className="w-full h-full object-cover"
                  src={src}
                  autoPlay
                  loop
                  muted
                  playsInline
                />
              </div>
            </div>
          ))}
        </div>
      </div>
      
      {/* --- Centered Login Box (On Top) --- */}
      {/* z-10 ensures this is on top of the background */}
      <div className="relative z-10 bg-black/40 backdrop-blur-xl p-8 rounded-2xl shadow-lg border border-white/20">
        <div className="text-center">
            <h1 className="text-white text-3xl font-bold mb-2">Shorts Creator</h1>
            <p className="text-gray-300 mb-6">Sign in to continue</p>
        </div>
        <Suspense fallback={
          <button
            disabled
            className="bg-gray-300 text-gray-500 font-semibold py-2 px-4 border border-gray-300 rounded-lg shadow-sm flex items-center gap-3"
          >
            Loading...
          </button>
        }>
          <GoogleSignInButton />
        </Suspense>
      </div>
    </div>
  );
}
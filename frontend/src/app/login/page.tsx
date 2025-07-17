'use client';

import { signIn } from 'next-auth/react';
import { useSearchParams } from 'next/navigation';
import Image from 'next/image';

// You can create a reusable Button component or use a UI library
const GoogleSignInButton = () => {
  const searchParams = useSearchParams();
  const callbackUrl = searchParams.get('callbackUrl') || '/dashboard';

  return (
    <button
      onClick={() => signIn('google', { callbackUrl })}
      className="bg-white text-gray-700 font-semibold py-2 px-4 border border-gray-300 rounded-lg shadow-sm hover:bg-gray-50 transition-colors flex items-center gap-3"
    >
      <Image src="/google-logo.svg" alt="Google logo" width={20} height={20} />
      Sign in with Google
    </button>
  );
};


export default function LoginPage() {
  return (
    // Your custom background and layout
    <div className="min-h-screen flex items-center justify-center bg-gray-900" style={{ backgroundImage: `url('/your-background-image.jpg')`, backgroundSize: 'cover' }}>
      <div className="bg-white/10 backdrop-blur-md p-8 rounded-xl shadow-lg border border-white/20">
        <h1 className="text-white text-3xl font-bold text-center mb-6">Welcome</h1>
        <GoogleSignInButton />
      </div>
    </div>
  );
}
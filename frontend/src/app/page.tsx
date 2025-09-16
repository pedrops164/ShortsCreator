'use client';

import React, { Suspense, useState } from 'react';
import { signIn } from 'next-auth/react';
import { useSearchParams } from 'next/navigation';
import Image from 'next/image';
import { Loader2, Clapperboard, MessageSquare, Wand2, UploadCloud, Download, ArrowRight } from 'lucide-react'; // Added ArrowRight for the redirect button

// --- Reusable Button Component ---

type SignInButtonProps = {
  isGoogleSignIn: boolean;
  text: string;
};

const SignInButton = ({ isGoogleSignIn, text }: SignInButtonProps) => {
  const searchParams = useSearchParams();
  const callbackUrl = searchParams.get('callbackUrl') || '/create';
  
  const [isLoading, setIsLoading] = useState(false);

  const handleAction = async () => {
    if (isGoogleSignIn) {
      setIsLoading(true);
      signIn('keycloak', { callbackUrl }, { kc_idp_hint: 'google' });
    } else {
      window.location.href = callbackUrl;
    }
  };

  return (
    <button
      onClick={handleAction}
      disabled={isLoading}
      className="relative w-full max-w-xs px-8 py-4 text-lg font-bold text-white transition-all duration-300 ease-in-out rounded-lg shadow-lg bg-gradient-to-r from-indigo-500 via-purple-500 to-pink-500 hover:scale-105 hover:shadow-purple-500/50 focus:outline-none focus:ring-2 focus:ring-purple-400 focus:ring-offset-2 focus:ring-offset-gray-900 disabled:opacity-70 disabled:cursor-not-allowed"
    >
      {isLoading ? (
        <div className="absolute inset-0 flex items-center justify-center">
          <Loader2 className="w-6 h-6 animate-spin" />
        </div>
      ) : (
        <div className="flex items-center justify-center gap-3">
          {isGoogleSignIn ? (
            <Image src="/google-logo.png" alt="Google logo" width={24} height={24} />
          ) : (
            <ArrowRight className="w-6 h-6" /> // A new, simple icon for the redirect button
          )}
          <span>{text}</span>
        </div>
      )}
    </button>
  );
};

// --- Page Sections ---

const AnimatedVideoGrid = () => {
  const imageSources = [
    'https://shortscreator-example-videos.s3.eu-north-1.amazonaws.com/whats_a_cheat_code.png',
    'https://shortscreator-example-videos.s3.eu-north-1.amazonaws.com/how_is_ai_trained.png',
    'https://shortscreator-example-videos.s3.eu-north-1.amazonaws.com/psychopath.png',
    'https://shortscreator-example-videos.s3.eu-north-1.amazonaws.com/bystander_effect.png',
    'https://shortscreator-example-videos.s3.eu-north-1.amazonaws.com/number_one_mistake.png',
    'https://shortscreator-example-videos.s3.eu-north-1.amazonaws.com/black_hole.png',
    'https://shortscreator-example-videos.s3.eu-north-1.amazonaws.com/whats_a_cheat_code.png',
    'https://shortscreator-example-videos.s3.eu-north-1.amazonaws.com/how_is_ai_trained.png',
  ];

  const column1Images = [...imageSources.slice(0, 4), ...imageSources.slice(0, 4)];
  const column2Images = [...imageSources.slice(4, 8), ...imageSources.slice(4, 8)];

  return (
    <div className="absolute inset-0 z-0 h-full w-full overflow-hidden [mask-image:radial-gradient(ellipse_100%_40%_at_50%_60%,black_10%,transparent_100%)]">
      <div className="absolute inset-0 flex items-center justify-center gap-4 opacity-30">
        <div className="flex h-full animate-scroll-y flex-col gap-4">
          {column1Images.map((src, index) => (
            <div key={`col1-${index}`} className="aspect-[9/16] w-36 md:w-48 rounded-2xl overflow-hidden shadow-2xl">
              <Image src={src} alt="" width={300} height={533} className="object-cover w-full h-full" priority />
            </div>
          ))}
        </div>
        <div className="hidden h-full animate-scroll-y-reverse md:flex flex-col gap-4">
          {column2Images.map((src, index) => (
            <div key={`col2-${index}`} className="aspect-[9/16] w-36 md:w-48 rounded-2xl overflow-hidden shadow-2xl">
              <Image src={src} alt="" width={300} height={533} className="object-cover w-full h-full" priority />
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};


const HeroSection = () => (
  <section className="relative flex flex-col items-center justify-center min-h-[80vh] px-4 text-center overflow-hidden bg-gray-900 bg-grid-white/[0.05]">
    <AnimatedVideoGrid />
    <div className="relative z-20 max-w-3xl">
      <h1 className="text-4xl font-extrabold tracking-tight text-transparent sm:text-5xl md:text-7xl bg-clip-text bg-gradient-to-br from-white to-gray-400">
        Transform Reddit Threads & Ideas into Viral Shorts. Instantly.
      </h1>
      <p className="max-w-xl mx-auto mt-6 text-lg text-gray-300 md:text-xl">
        Stop spending hours editing. Our AI video generator creates captivating, ready-to-post content for TikTok, YouTube Shorts, and Reels in minutes.
      </p>
      <div className="mt-10">
        <Suspense fallback={
          <button disabled className="px-8 py-4 text-lg font-bold text-white rounded-lg bg-gray-600">
            Loading...
          </button>
        }>
          <SignInButton isGoogleSignIn={false} text="Start Creating" />
        </Suspense>
      </div>
    </div>
  </section>
);

type FeatureCardProps = {
  icon: React.ElementType;
  title: string;
  description: string;
  imageSrc: string;
  alt: string;
};

const FeatureCard = ({ icon: Icon, title, description, imageSrc, alt }: FeatureCardProps) => (
    <div className="overflow-hidden transition-all duration-300 border rounded-2xl bg-gray-900/50 border-white/10 backdrop-blur-lg hover:border-purple-500/50 hover:shadow-2xl hover:shadow-purple-500/20 hover:-translate-y-1">
      <div className="grid grid-cols-1 md:grid-cols-5 md:gap-6 items-center">
        <div className="p-6 md:p-8 md:col-span-3">
          <div className="flex items-center gap-4 mb-4">
            <div className="p-2 rounded-lg bg-white/10">
              <Icon className="w-6 h-6 text-purple-400" />
            </div>
            <h3 className="text-2xl font-bold text-white">{title}</h3>
          </div>
          <p className="text-gray-400">{description}</p>
        </div>
        <div className="px-6 pb-6 md:p-6 md:col-span-2">
            <div className="relative w-full max-w-[220px] mx-auto md:max-w-full aspect-[9/16] overflow-hidden rounded-xl shadow-inner shadow-black/50 ring-1 ring-white/10">
                <Image 
                    src={imageSrc} 
                    alt={alt} 
                    layout="fill" 
                    className="object-cover"
                />
            </div>
        </div>
      </div>
    </div>
);

const FeaturesSection = () => (
    <section className="relative z-10 w-full max-w-6xl px-4 py-20 mx-auto sm:py-28">
      <div className="text-center">
          <h2 className="text-3xl font-extrabold tracking-tight text-white sm:text-4xl">Two Viral Formats, One Click Away</h2>
          <p className="max-w-2xl mx-auto mt-4 text-lg text-gray-400">
            Tap into proven content styles that dominate short-form video platforms.
          </p>
      </div>
      <div className="grid grid-cols-1 gap-8 mt-12 md:grid-cols-2">
        <FeatureCard 
            icon={MessageSquare}
            title="Reddit Stories"
            description="Turn captivating Reddit threads into engaging videos. Our AI finds the best stories, adds an emotive voiceover, and syncs it with engaging background gameplay."
            imageSrc="https://shortscreator-example-videos.s3.eu-north-1.amazonaws.com/psychopath.png"
            alt="Example of a Reddit Story video being created"
        />
        <FeatureCard 
            icon={Clapperboard}
            title="Character Explains"
            description="Explain any topic with the voices of iconic characters. Create hilarious and shareable content with pairs like Rick & Morty, Peter & Stewie, and more."
            imageSrc="https://shortscreator-example-videos.s3.eu-north-1.amazonaws.com/how_is_ai_trained.png"
            alt="Example of a Character Explains video"
        />
      </div>
    </section>
);

const HowItWorksSection = () => (
    <section className="relative z-10 w-full max-w-5xl px-4 py-20 mx-auto sm:py-28">
        <div className="text-center">
            <h2 className="text-3xl font-extrabold tracking-tight text-white sm:text-4xl">Create in 3 Simple Steps</h2>
        </div>
        <div className="grid grid-cols-1 gap-12 mt-16 md:grid-cols-3">
            <div className="flex flex-col items-center text-center">
                <div className="flex items-center justify-center w-16 h-16 mb-4 text-purple-300 border-2 rounded-full border-purple-500/50 bg-gray-900/50">
                    <UploadCloud className="w-8 h-8" />
                </div>
                <h3 className="text-xl font-semibold text-white">1. Provide Content</h3>
                <p className="mt-2 text-gray-400">Paste a Reddit URL or just write your topic and choose characters.</p>
            </div>
            <div className="flex flex-col items-center text-center">
                <div className="flex items-center justify-center w-16 h-16 mb-4 text-purple-300 border-2 rounded-full border-purple-500/50 bg-gray-900/50">
                    <Wand2 className="w-8 h-8" />
                </div>
                <h3 className="text-xl font-semibold text-white">2. Let AI Work Its Magic</h3>
                <p className="mt-2 text-gray-400">Our system generates the script, voiceover, subtitles, and finds background video.</p>
            </div>
            <div className="flex flex-col items-center text-center">
                <div className="flex items-center justify-center w-16 h-16 mb-4 text-purple-300 border-2 rounded-full border-purple-500/50 bg-gray-900/50">
                    <Download className="w-8 h-8" />
                </div>
                <h3 className="text-xl font-semibold text-white">3. Download & Post</h3>
                <p className="mt-2 text-gray-400">Receive your high-quality short video, ready to be uploaded and go viral.</p>
            </div>
        </div>
    </section>
);

// --- Main Page Component ---

export default function LoginPage() {
  return (
    <main className="w-full min-h-screen bg-black">
      <HeroSection />
      <FeaturesSection />
      <HowItWorksSection />
      <section className="py-20 text-center sm:py-28">
          <h2 className="text-3xl font-extrabold tracking-tight text-white sm:text-4xl">Ready to Go Viral?</h2>
          <p className="max-w-xl mx-auto mt-4 text-lg text-gray-300">Start creating engaging short-form content today. It&apos;s free to get started.</p>
          <div className="mt-10">
              <Suspense fallback={
                <button disabled className="px-8 py-4 text-lg font-bold text-white rounded-lg bg-gray-600">
                  Loading...
                </button>
              }>
                <SignInButton isGoogleSignIn={true} text="Sign in with Google" />
              </Suspense>
          </div>
      </section>
    </main>
  );
}
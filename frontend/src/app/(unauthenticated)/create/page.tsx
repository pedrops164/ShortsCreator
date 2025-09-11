'use client';

import React from 'react';
import { useRouter } from 'next/navigation';
import { Lock } from 'lucide-react'; // Using icons for better UI

// --- Updated Data Structure ---
const contentTemplates = [
  {
    id: 'reddit-story',
    title: 'Reddit Story',
    description: 'Turn any Reddit thread into a viral video with compelling narration and gameplay.',
    videoSrc: 'https://shortscreator-example-videos.s3.eu-north-1.amazonaws.com/Whats+something+someone+did+that+convinced+you+they+were+a+complete+psycopath_compressed.mp4',
    createUrl: '/editor/create/reddit_story_v1',
    status: 'available',
  },
  {
    id: 'character-explains',
    title: 'Character Explains',
    description: "An animated character breaks down complex topics in a simple, engaging way.",
    videoSrc: 'https://shortscreator-example-videos.s3.eu-north-1.amazonaws.com/How+is+AI+trained_compressed.mp4',
    createUrl: '/editor/create/character_explains_v1',
    status: 'available',
  },
  {
    id: 'coming-soon-1',
    title: 'AI News Anchor',
    description: 'Generate daily news shorts with a realistic AI news anchor delivering the script.',
    videoSrc: 'https://shortscreator-example-videos.s3.eu-north-1.amazonaws.com/Whats+something+someone+did+that+convinced+you+they+were+a+complete+psycopath_compressed.mp4', // Placeholder video
    createUrl: '#',
    status: 'coming-soon',
  },
];

// --- Type Definition ---
type Template = (typeof contentTemplates)[0];

// --- Finalized Template Card Component ---
const TemplateCard = ({ template }: { template: Template }) => {
  const router = useRouter();
  const isAvailable = template.status === 'available';

  const handleCardClick = () => {
    if (isAvailable) {
      router.push(template.createUrl);
    }
  };

  return (
    <div
      onClick={handleCardClick}
      className={`group flex flex-col bg-card border border-border rounded-xl overflow-hidden
                 shadow-sm transition-all duration-300 ease-in-out
                 ${isAvailable
                   ? 'cursor-pointer hover:shadow-md hover:border-primary/50 hover:-translate-y-1'
                   : 'cursor-default'
                 }`}
    >
      {/* Video preview */}
      <div className="relative w-full aspect-[9/16] bg-neutral-900 p-2">
        <video
          key={template.videoSrc}
          className="w-full h-full object-cover rounded-lg"
          src={template.videoSrc}
          autoPlay
          loop
          muted
          playsInline
        />
        {!isAvailable && (
          <div className="absolute inset-0 bg-black/50 backdrop-blur-sm flex flex-col items-center justify-center rounded-lg">
            <Lock className="w-8 h-8 text-white/80" />
            <span className="mt-2 text-sm font-semibold text-white">Coming Soon</span>
          </div>
        )}
      </div>

      {/* Content Section */}
      <div className="p-5">
        <h3 className="text-lg font-semibold text-foreground text-center">{template.title}</h3>
        <p className="mt-2 text-sm text-muted-foreground text-center">
          {template.description}
        </p>
      </div>
    </div>
  );
};

// --- Main Page Component ---
export default function CreateVideoPage() {
  return (
    <main className="flex-1 bg-background">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 sm:py-12">
        <header className="mb-10 text-center">
          <h1 className="text-4xl sm:text-5xl font-extrabold text-foreground tracking-tight">
            Choose Your Template
          </h1>
        </header>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          {contentTemplates.map((template) => (
            <TemplateCard key={template.id} template={template} />
          ))}
        </div>
      </div>
    </main>
  );
}
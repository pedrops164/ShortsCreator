'use client';

import React from 'react';
import { useRouter } from 'next/navigation';

// --- Data for Content Templates ---
const contentTemplates = [
  {
    id: 'reddit-story',
    title: 'Reddit Story',
    description: 'Turn Reddit threads into engaging videos with background gameplay.',
    videoSrc: '/videos/test_reddit_story.mp4',
    createUrl: '/editor/create/reddit_story_v1',
  },
  {
    id: 'another-template',
    title: 'Coming Soon',
    description: 'A new template will be available in the near future.',
    videoSrc: '/videos/reddit-example.mp4', // Placeholder
    createUrl: '#',
  },
  {
    id: 'another-templatee',
    title: 'Coming Soon',
    description: 'A new template will be available in the near future.',
    videoSrc: '/videos/reddit-example.mp4', // Placeholder
    createUrl: '#',
  },
  // Add up to 4 templates to see the full row
];

// --- Template Card Component ---
const TemplateCard = ({ template }: { template: typeof contentTemplates[0] }) => {
  const router = useRouter();
  const isClickable = template.createUrl !== '#';

  const handleClick = () => {
    if (isClickable) {
      router.push(template.createUrl);
    }
  };

  return (
    <div
      onClick={handleClick}
      className={`group flex flex-col items-center text-center p-4 rounded-xl shadow-lg transition-all duration-300 bg-background/50 border border-accent/50 ${isClickable ? 'cursor-pointer hover:border-primary hover:shadow-primary/20 hover:-translate-y-1' : 'opacity-70'}`}
    >
      {/* Title above the video */}
      <h3 className="text-xl font-bold text-foreground mb-4">{template.title}</h3>

      {/* Phone Mockup for Video */}
      <div className="w-48 h-[320px] bg-gray-900 rounded-3xl p-2 border-4 border-gray-700 overflow-hidden">
        <video
          key={template.videoSrc}
          className="w-full h-full object-cover rounded-2xl"
          src={template.videoSrc}
          autoPlay
          loop
          muted
          playsInline
        />
      </div>

      {/* Description below the video */}
      <p className="mt-4 text-sm text-accent max-w-xs">
        {template.description}
      </p>
    </div>
  );
};


// --- Main Page Component ---
export default function CreateVideoPage() {
  return (
    <main className="flex-1 p-4 sm:p-6 lg:p-8">
      <div className="max-w-7xl mx-auto">
        <header className="mb-12 text-center">
          <h2 className="text-3xl font-bold text-foreground tracking-tight">Create a New Video</h2>
          <p className="text-accent mt-1">
            Choose a content template to get started.
          </p>
        </header>

        {/* Responsive grid for template cards, centered */}
        {/*
          We wrap the grid in a flex container.
          - `flex justify-center`: This outer container's only job is to center its child.
          - `flex-wrap`: Allows items to wrap to the next line.
        */}
        <div className="flex flex-wrap justify-center gap-8">
          {contentTemplates.map((template) => (
            <TemplateCard key={template.id} template={template} />
          ))}
        </div>
      </div>
    </main>
  );
}
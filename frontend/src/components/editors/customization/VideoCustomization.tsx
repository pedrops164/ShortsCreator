// src/components/editors/customization/VideoCustomization.tsx
'use client';

import React from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import Image from 'next/image';
import { Film, Mic2, Music, Paintbrush } from 'lucide-react';

// --- Main Wrapper Component ---
function VideoCustomization({ children }: { children: React.ReactNode }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Video Customization</CardTitle>
        <CardDescription>Adjust the core visual and audio elements of your video.</CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        {children}
      </CardContent>
    </Card>
  );
}

// --- Data Sources (kept internally for reusability) ---
const BACKGROUND_VIDEOS = [
  { id: 'minecraft1', name: 'Minecraft Gameplay', thumbnail: '/video_thumbnails/minecraft1.png', category: 'Gaming' },
  { id: 'subway_surfers1', name: 'Subway Surfers', thumbnail: '/video_thumbnails/subway_surfers1.png', category: 'Gaming' },
];

const BACKGROUND_MUSIC = [
    { id: 'none', name: 'None' },
    { id: 'upbeat_1', name: 'Fun & Upbeat' },
    { id: 'mysterious_1', name: 'Mysterious Vibe' },
    { id: 'lofi_1', name: 'Lofi Beats' },
];

const NARRATION_VOICES = [
    { id: 'openai_alloy', name: 'Alloy (Neutral)'},
    { id: 'openai_shimmer', name: 'Shimmer (Female)'},
    { id: 'openai_onyx', name: 'Onyx (Male)'},
    { id: 'openai_nova', name: 'Nova (Female)'},
];

const THEMES = [
    { id: 'dark', name: 'Dark Mode' },
    { id: 'light', name: 'Light Mode' },
];

interface SelectorProps {
  value: string;
  onChange: (value: string) => void;
  error?: string;
}

// --- Sub-Components (not exported individually) ---

const BackgroundVideo = ({ value, onChange, error }: SelectorProps) => (
    <div className={error ? 'rounded-md border border-destructive p-2' : ''}>
        <Label className="font-semibold flex items-center mb-2"><Film className="w-4 h-4 mr-2" />Background Video *</Label>
        <div className="grid grid-cols-2 gap-3">
            {BACKGROUND_VIDEOS.map((bg) => (
            <div key={bg.id} onClick={() => onChange(bg.id)}
                className={`relative cursor-pointer rounded-lg border-2 p-2 transition-all ${ value === bg.id ? 'border-primary bg-primary/5' : 'border-border hover:border-primary/50' }`}>
                
                {/* New: Use a smaller container for the image.
                  The `w-[120px]` and `h-[67.5px]` classes give a 16:9 aspect ratio 
                  and a smaller fixed size. You can adjust these numbers to your liking.
                  `overflow-hidden` is important to contain the image.
                */}
                <div className="relative w-full h-20 aspect-video rounded-md overflow-hidden bg-muted">
                    <Image 
                        src={bg.thumbnail} 
                        alt={bg.name} 
                        fill={true} 
                        className="object-cover" 
                    />
                </div>
                
                <div className="mt-2">
                    <p className="text-sm font-medium text-foreground truncate">{bg.name}</p>
                    <p className="text-xs text-muted-foreground">{bg.category}</p>
                </div>
                
                {value === bg.id && (<div className="absolute top-2 right-2"><Badge className="text-xs">Selected</Badge></div>)}
            </div>
            ))}
        </div>
        {error && <p className="text-sm text-destructive mt-2">{error}</p>}
    </div>
);

const MusicOptions = ({ value, onChange, error }: SelectorProps) => (
    <div>
        <Label htmlFor="music-select" className="font-semibold flex items-center"><Music className="w-4 h-4 mr-2" />Background Music</Label>
        <Select value={value} onValueChange={onChange}>
            <SelectTrigger id="music-select" className={`mt-2 ${error ? 'border-destructive' : ''}`}><SelectValue placeholder="Select music..." /></SelectTrigger>
            <SelectContent>{BACKGROUND_MUSIC.map(music => (<SelectItem key={music.id} value={music.id}>{music.name}</SelectItem>))}</SelectContent>
        </Select>
        {error && <p className="text-sm text-destructive mt-2">{error}</p>}
    </div>
);

const Voice = ({ value, onChange, error }: SelectorProps) => (
    <div>
        <Label htmlFor="voice-select" className="font-semibold flex items-center"><Mic2 className="w-4 h-4 mr-2" />Narration Voice</Label>
        <Select value={value} onValueChange={onChange}>
            <SelectTrigger id="voice-select" className={`mt-2 ${error ? 'border-destructive' : ''}`}><SelectValue placeholder="Select a voice..." /></SelectTrigger>
            <SelectContent>{NARRATION_VOICES.map(voice => (<SelectItem key={voice.id} value={voice.id}>{voice.name}</SelectItem>))}</SelectContent>
        </Select>
        {error && <p className="text-sm text-destructive mt-2">{error}</p>}
    </div>
);

const Theme = ({ value, onChange, error }: SelectorProps) => (
    <div>
        <Label htmlFor="theme-select" className="font-semibold flex items-center"><Paintbrush className="w-4 h-4 mr-2" />Theme</Label>
        <Select value={value} onValueChange={onChange}>
            <SelectTrigger id="theme-select" className={`mt-2 ${error ? 'border-destructive' : ''}`}><SelectValue placeholder="Select a theme..." /></SelectTrigger>
            <SelectContent>{THEMES.map(theme => (<SelectItem key={theme.id} value={theme.id}>{theme.name}</SelectItem>))}</SelectContent>
        </Select>
        {error && <p className="text-sm text-destructive mt-2">{error}</p>}
    </div>
);


// Attach the sub-components as properties to the main component
VideoCustomization.BackgroundVideo = BackgroundVideo;
VideoCustomization.MusicOptions = MusicOptions;
VideoCustomization.Voice = Voice;
VideoCustomization.Theme = Theme;

export { VideoCustomization };
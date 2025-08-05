'use client';

import React, { useState, useEffect, useRef } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import Image from 'next/image';
import { Film, Mic2, Music, Paintbrush, Play, Pause, Check } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { VoiceAsset, VideoAsset } from '@/types'; // Adjust the import path as needed
import apiClient from '@/lib/apiClient';

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

const BackgroundVideo = ({ value, onChange, error }: SelectorProps) => {
  const [availableVideos, setAvailableVideos] = useState<VideoAsset[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    async function fetchVideos() {
      try {
        const response = await apiClient.get<VideoAsset[]>("assets?type=VIDEO");
        const data: VideoAsset[] = response.data;
        setAvailableVideos(data);
      } catch (error) {
        console.error("Error fetching video assets:", error);
      } finally {
        setLoading(false);
      }
    }

    fetchVideos();
  }, []);

  if (loading) return <div className="p-4 text-center">Loading background videos...</div>;
  
  return (
    <div className={error ? 'rounded-md border border-destructive p-2' : ''}>
        <Label className="font-semibold flex items-center mb-2"><Film className="w-4 h-4 mr-2" />Background Video *</Label>
        <div className="grid grid-cols-2 gap-3">
            {availableVideos.map((bg) => (
            <div key={bg.id} onClick={() => onChange(bg.assetId)}
                className={`relative cursor-pointer rounded-lg border-2 p-2 transition-all ${ value === bg.assetId ? 'border-primary bg-primary/5' : 'border-border hover:border-primary/50' }`}>
                
                <div className="relative w-full h-20 aspect-video rounded-md overflow-hidden bg-muted">
                    <Image 
                        src={bg.thumbnailUrl} 
                        alt={bg.name} 
                        fill={true} 
                        className="object-cover" 
                    />
                </div>
                
                <div className="mt-2">
                    <p className="text-sm font-medium text-foreground truncate">{bg.name}</p>
                    <p className="text-xs text-muted-foreground">{bg.category}</p>
                </div>
                
                {value === bg.assetId && (<div className="absolute top-2 right-2"><Badge className="text-xs">Selected</Badge></div>)}
            </div>
            ))}
        </div>
        {error && <p className="text-sm text-destructive mt-2">{error}</p>}
    </div>
  );
};

/* const MusicOptions = ({ value, onChange, error }: SelectorProps) => (
    <div>
        <Label htmlFor="music-select" className="font-semibold flex items-center"><Music className="w-4 h-4 mr-2" />Background Music</Label>
        <Select value={value} onValueChange={onChange}>
            <SelectTrigger id="music-select" className={`mt-2 ${error ? 'border-destructive' : ''}`}><SelectValue placeholder="Select music..." /></SelectTrigger>
            <SelectContent>{BACKGROUND_MUSIC.map(music => (<SelectItem key={music.id} value={music.id}>{music.name}</SelectItem>))}</SelectContent>
        </Select>
        {error && <p className="text-sm text-destructive mt-2">{error}</p>}
    </div>
); */

const Voice = ({ value, onChange, error }: SelectorProps) => {
  const [isOpen, setIsOpen] = useState(false);
  const [playingId, setPlayingId] = useState<string | null>(null);
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const [available_voices, setAvailableVoices] = useState<VoiceAsset[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  //const [error, setError] = useState<string | null>(null);

  const selectedVoice = available_voices.find(v => v.assetId === value);

  useEffect(() => {
    async function fetchVoices() {
      try {
          // The URL points to your gateway, which routes to the CSS
          const response = await apiClient.get<VoiceAsset[]>("assets?type=VOICE");
          const data: VoiceAsset[] = response.data;
          setAvailableVoices(data);
          console.log("Fetched voices:", data);
      } catch (err) {
          console.error("Failed to fetch voices", err);
          //setError(err instanceof Error ? err.message : 'An unknown error occurred');
      } finally {
          setLoading(false);
      }
    }

    fetchVoices();
  }, []);

  // Cleanup function to stop audio when the component unmounts or dialog closes
  useEffect(() => {
    return () => {
      if (audioRef.current) {
        audioRef.current.pause();
        audioRef.current = null;
      }
    };
  }, []);

  const handlePlayPause = (voiceId: string, sourceUrl: string) => {
    console.log("handlePlayPause called with voiceId:", voiceId, "sourceUrl:", sourceUrl);
    if (playingId === voiceId) {
      // Pause the current audio
      audioRef.current?.pause();
      setPlayingId(null);
    } else {
      // Stop any previously playing audio
      if (audioRef.current) {
        audioRef.current.pause();
      }
      // Start new audio
      const newAudio = new Audio(sourceUrl);
      audioRef.current = newAudio;
      setPlayingId(voiceId);
      newAudio.play();
      newAudio.onended = () => {
        setPlayingId(null);
      };
    }
  };

  const handleSelectVoice = (voiceId: string) => {
    onChange(voiceId);
    setIsOpen(false);
    // Stop audio on selection
    if (audioRef.current) {
        audioRef.current.pause();
        setPlayingId(null);
    }
  };

  if (loading) return <div className="p-4 text-center">Loading voices...</div>;

  return (
    <div>
      <Label className="font-semibold flex items-center"><Mic2 className="w-4 h-4 mr-2" />Narration Voice</Label>
      <Dialog open={isOpen} onOpenChange={setIsOpen}>
        <DialogTrigger asChild>
          <Button variant="outline" className={`mt-2 w-full justify-start font-normal ${error ? 'border-destructive' : ''}`}>
            {selectedVoice ? selectedVoice.name : "Select a voice..."}
          </Button>
        </DialogTrigger>
        <DialogContent className="sm:max-w-[425px]">
          <DialogHeader>
            <DialogTitle>Choose Narration Voice</DialogTitle>
            <DialogDescription>
              Listen to a sample and select the voice for your video narration.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-3 py-2 max-h-[60vh] overflow-y-auto pr-2">
            {available_voices.map((voice) => (
              <div
                key={voice.id}
                className={`flex items-center space-x-4 rounded-md border p-4 transition-colors ${value === voice.assetId ? 'border-primary bg-primary/5' : ''}`}
              >
                <Button
                  variant="outline"
                  size="icon"
                  onClick={() => handlePlayPause(voice.assetId, voice.sourceUrl)}
                  className="flex-shrink-0"
                >
                  {playingId === voice.assetId ? <Pause className="h-4 w-4" /> : <Play className="h-4 w-4" />}
                </Button>
                <div className="flex-grow">
                  <p className="font-semibold">{voice.name}</p>
                  <p className="text-sm text-muted-foreground">{voice.description}</p>
                </div>
                <Button
                  variant={value === voice.assetId ? "default" : "secondary"}
                  onClick={() => handleSelectVoice(voice.assetId)}
                  size="sm"
                >
                  {value === voice.assetId ? <Check className="h-4 w-4 mr-2" /> : null}
                  Select
                </Button>
              </div>
            ))}
          </div>
        </DialogContent>
      </Dialog>
      {error && <p className="text-sm text-destructive mt-2">{error}</p>}
    </div>
  );
};

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
/* VideoCustomization.MusicOptions = MusicOptions; */
VideoCustomization.Voice = Voice;
VideoCustomization.Theme = Theme;

export { VideoCustomization };
'use client';

import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Switch } from '@/components/ui/switch';
import { Separator } from '@/components/ui/separator';
import { Check } from 'lucide-react';
import apiClient from '@/lib/apiClient'; // Assuming you have this configured
import { FontAsset } from '@/types/assets'; // Adjust the import path as needed

// --- Type Definition ---
// This should match the structure in the `CharacterExplainsParams` type
export interface SubtitleSettings {
  show: boolean;
  font: string;
  color: string;
  position: string;
}

// --- Prop Definition ---
interface SubtitleOptionsProps {
  value: SubtitleSettings;
  onChange: (field: keyof SubtitleSettings, value: any) => void;
  hasErrors?: boolean; 
}

// --- Helper Function to determine font format from URL ---
const getFontFormat = (url: string): string | null => {
    const extension = url.split('.').pop()?.toLowerCase();
    switch (extension) {
        case 'ttf': return 'truetype';
        case 'otf': return 'opentype';
        case 'woff': return 'woff';
        case 'woff2': return 'woff2';
        default: return null; // Or 'truetype' as a fallback if you only use TTF
    }
};

export function SubtitleOptions({ value, onChange, hasErrors }: SubtitleOptionsProps) {
  const [availableFonts, setAvailableFonts] = useState<FontAsset[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  // Effect to fetch font assets from the backend
  useEffect(() => {
    async function fetchFonts() {
      try {
        const response = await apiClient.get<FontAsset[]>("/assets?type=FONT");
        console.log("Fetched font assets:", response.data);
        setAvailableFonts(response.data);
      } catch (error) {
        console.error("Error fetching font assets:", error);
        // Handle error state in UI if necessary
      } finally {
        setLoading(false);
      }
    }

    fetchFonts();
  }, []);

  // Effect to dynamically inject @font-face rules into the document head
  useEffect(() => {
    if (availableFonts.length > 0) {
      const styleId = 'dynamic-font-styles';
      if (document.getElementById(styleId)) {
        // To handle potential updates if fonts can change, you might want to replace the content
        document.getElementById(styleId)!.innerHTML = ''; 
      }

      const style = document.getElementById(styleId) || document.createElement('style');
      style.id = styleId;

      style.innerHTML = availableFonts
        .map((font) => {
          const format = getFontFormat(font.sourceUrl);
          // Only generate a rule if the format is recognized
          if (!format) {
            console.warn(`Could not determine font format for ${font.name} from URL: ${font.sourceUrl}`);
            return '';
          }
          return `
            @font-face {
              font-family: '${font.name}';
              src: url('${font.sourceUrl}') format('${format}');
              font-weight: normal;
              font-style: normal;
              font-display: swap;
            }
          `;
        })
        .join('\n');
      
      if (!document.getElementById(styleId)) {
          document.head.appendChild(style);
      }
    }
  }, [availableFonts]);

  return (
    <Card className={hasErrors ? 'border-destructive' : ''}>
      <CardHeader>
        <CardTitle>Subtitle Customization</CardTitle>
        <CardDescription>Adjust the appearance and font of the text captions.</CardDescription>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <Label htmlFor="show-subtitles" className="font-semibold">
              Enable Subtitles
            </Label>
            <Switch
              id="show-subtitles"
              checked={value.show}
              onCheckedChange={(checked) => onChange('show', checked)}
            />
          </div>
          {value.show && (
            <>
              <Separator />
              <div className="space-y-6 pt-4">
                {/* --- Top Section: Appearance --- */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <Label className="text-xs font-semibold text-muted-foreground">Text Color</Label>
                    <div className="flex items-center space-x-2 mt-1">
                      <Input
                        type="color"
                        value={value.color}
                        onChange={(e) => onChange('color', e.target.value)}
                        className="p-1 h-10 w-10 cursor-pointer"
                      />
                      <Input
                        value={value.color.toUpperCase()}
                        onChange={(e) => onChange('color', e.target.value)}
                        className="w-24 bg-muted border"
                        aria-label="Color Hex Code"
                      />
                    </div>
                  </div>
                  <div>
                    <Label className="text-xs font-semibold text-muted-foreground">Position</Label>
                    <Select value={value.position} onValueChange={(pos: 'top' | 'center') => onChange('position', pos)}>
                      <SelectTrigger className="mt-1"><SelectValue /></SelectTrigger>
                      <SelectContent>
                        <SelectItem value="top">Top</SelectItem>
                        <SelectItem value="center">Center</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                </div>

                {/* --- Bottom Section: Font Selection --- */}
                <div>
                  <Label className="text-xs font-semibold text-muted-foreground">Font</Label>
                  {loading ? (
                    <div className="text-center p-4 text-muted-foreground">Loading fonts...</div>
                  ) : (
                    <div className="grid grid-cols-2 gap-3 mt-2">
                      {availableFonts.map((font) => (
                        <div
                          key={font.id}
                          onClick={() => onChange('font', font.name)}
                          className={`relative cursor-pointer rounded-lg border-2 p-2 transition-all ${                            value.font === font.name
                              ? 'border-primary bg-primary/5'
                              : 'border-border hover:border-primary/50 bg-background'
                          }`}
                        >
                          <div className="flex h-16 w-full items-center justify-center rounded-md bg-muted">
                            <p className="text-2xl" style={{ fontFamily: `'${font.name}', sans-serif` }}>
                              THIS IS AN EXAMPLE TEXT
                            </p>
                          </div>
                          <div className="mt-2">
                            <p className="truncate text-center text-sm font-medium text-foreground">
                              {font.displayName}
                            </p>
                          </div>
                          {value.font === font.name && (
                            <div className="absolute right-2 top-2 flex h-5 w-5 items-center justify-center rounded-full bg-primary text-primary-foreground">
                              <Check className="h-3.5 w-3.5" />
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
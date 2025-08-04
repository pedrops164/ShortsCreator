'use client';

import { useState, useMemo, useEffect, useRef } from 'react';
import { CharacterExplainsDraft, CharacterExplainsParams } from '@/types'; // Assuming types are in @/types
import {
  Save, Send, Plus, Trash2, AlertCircle, Users, MessageSquare, ArrowLeft, Loader2
} from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Alert, AlertDescription } from '@/components/ui/alert';
import Image from 'next/image';
import { SubtitleOptions } from './customization/SubtitleOptions';
import { VideoCustomization } from './customization/VideoCustomization';

// --- Type Definitions ---
interface Character {
  id: string;
  name: string;
  avatar: string;
  color: string;
}

interface CharacterPreset {
  id: string;
  name: string;
  characters: Character[];
  thumbnail: string;
}

interface DialogueLine {
  characterId: string;
  text: string;
}

// --- Prop Definition ---
interface EditorProps {
  initialData: CharacterExplainsDraft;
  onSave: (data: CharacterExplainsDraft) => void;
  onSubmit: (data: CharacterExplainsDraft) => void;
  isSaving: boolean;
  isSubmitting: boolean;
}

// --- Mock Data ---
const PRESETS: CharacterPreset[] = [
  {
    id: 'peter_stewie', name: 'Peter & Stewie',
    characters: [
      { id: 'peter', name: 'Peter Griffin', avatar: '/character_images/peter.png', color: '#3b82f6' },
      { id: 'stewie', name: 'Stewie Griffin', avatar: '/character_images/stewie.png', color: '#ef4444' }
    ],
    thumbnail: '/preset_thumbnails/peter_stewie.png'
  },
  {
    id: 'rick_morty', name: 'Rick & Morty',
    characters: [
      { id: 'rick', name: 'Rick Sanchez', avatar: '/character_images/rick.png', color: '#2dd4bf' },
      { id: 'morty', name: 'Morty Smith', avatar: '/character_images/morty.png', color: '#facc15' },
    ],
    thumbnail: '/preset_thumbnails/rick_morty.png',
  },
];

// --- Default values for a new draft ---
const defaultParams: CharacterExplainsParams = {
  characterPresetId: '',
  topicTitle: '',
  dialogue: [],
  backgroundVideoId: 'minecraft1',
  aspectRatio: '9:16',
  subtitles: { show: true, color: '#FFFFFF', font: 'Arial', position: 'bottom' },
};

// --- Validation Logic ---
const validate = (params: CharacterExplainsParams): Record<string, any> => {
  const errors: Record<string, any> = {};
  if (!params.characterPresetId) errors.characterPresetId = 'A character preset must be selected.';
  if (!params.topicTitle?.trim() || params.topicTitle.length < 3) errors.topicTitle = 'Topic Title must be at least 3 characters.';
  if (params.dialogue.length === 0) errors.dialogue = 'Dialogue script cannot be empty.';
  if (!params.backgroundVideoId) errors.backgroundVideoId = 'A background video must be selected.';
  return errors;
};

// --- Main Editor Component ---
export function CharacterExplainsEditor({ initialData, onSave, onSubmit, isSaving, isSubmitting }: EditorProps) {
  const [params, setParams] = useState<CharacterExplainsParams>({
    ...defaultParams,
    ...initialData.templateParams,
    subtitles: { ...defaultParams.subtitles, ...initialData.templateParams?.subtitles },
  });

  const [errors, setErrors] = useState<Record<string, any>>({});
  
  // Local state for the dialogue input form
  const [newDialogueText, setNewDialogueText] = useState('');
  const [activeSpeakerId, setActiveSpeakerId] = useState<string>('');

  const dialogueScrollRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // Memoize the active preset and characters for efficiency
  const activePreset = useMemo(() => PRESETS.find(p => p.id === params.characterPresetId), [params.characterPresetId]);
  
  // --- Effects ---

  // Scroll dialogue to bottom on new line
  useEffect(() => {
    if (dialogueScrollRef.current) {
      dialogueScrollRef.current.scrollTop = dialogueScrollRef.current.scrollHeight;
    }
  }, [params.dialogue]);

  // Set the default active speaker when the preset changes
  useEffect(() => {
    if (activePreset && activePreset.characters.length > 0) {
      if (!activeSpeakerId || !activePreset.characters.some(c => c.id === activeSpeakerId)) {
        setActiveSpeakerId(activePreset.characters[0].id);
      }
    } else {
      setActiveSpeakerId('');
    }
  }, [activePreset, activeSpeakerId]);

  // --- Handlers ---
  
  const handlePresetSelect = (preset: CharacterPreset) => {
    setParams(p => ({ ...p, characterPresetId: preset.id, dialogue: [] })); // Reset dialogue on preset change
    if (errors.characterPresetId) {
        setErrors(currentErrors => {
            const { characterPresetId, ...rest } = currentErrors;
            return rest;
        });
    }
  };
  
  const addDialogueLine = () => {
    if (!newDialogueText.trim() || !activeSpeakerId) return;
    const newLine: DialogueLine = { characterId: activeSpeakerId, text: newDialogueText.trim() };
    setParams(p => ({ ...p, dialogue: [...p.dialogue, newLine] }));
    setNewDialogueText('');

    if (errors.dialogue) {
        setErrors(currentErrors => {
            const { dialogue, ...rest } = currentErrors;
            return rest;
        });
    }

    // Auto-alternate speaker
    if (activePreset) {
      const currentIndex = activePreset.characters.findIndex(c => c.id === activeSpeakerId);
      const nextIndex = (currentIndex + 1) % activePreset.characters.length;
      setActiveSpeakerId(activePreset.characters[nextIndex].id);
    }
    textareaRef.current?.focus();
  };

  const removeDialogueLine = (indexToRemove: number) => {
    setParams(p => ({ ...p, dialogue: p.dialogue.filter((_, index) => index !== indexToRemove) }));
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      addDialogueLine();
    }
  };

  const handleFormChange = (field: keyof CharacterExplainsParams, value: any) => {
    setParams(p => ({ ...p, [field]: value }));
    if (errors[field]) {
      setErrors(currentErrors => {
        const { [field]: removedError, ...rest } = currentErrors;
        return rest;
      });
    }
  };

  const handleSubtitleChange = (field: keyof NonNullable<CharacterExplainsParams['subtitles']>, value: any) => {
    setParams(p => ({ ...p, subtitles: { ...p.subtitles, [field]: value } }));
  };

  const handleSave = () => {
    onSave({ ...initialData, templateParams: params });
  };

  const handleSubmit = () => {
    const validationErrors = validate(params);
    setErrors(validationErrors);
    if (Object.keys(validationErrors).length === 0) {
      onSubmit({ ...initialData, templateParams: params });
    } else {
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  const getCharacterById = (id: string) => activePreset?.characters.find(c => c.id === id);

  return (
    <div className="bg-background min-h-screen p-4 md:p-6 lg:p-8">
      <div className="max-w-7xl mx-auto space-y-8">
        {/* Header */}
        <div className="flex flex-wrap gap-4 items-center justify-between">
          <div className="flex items-center space-x-4">
            <Button variant="ghost" size="icon" asChild>
              <a href="/dashboard/content"><ArrowLeft className="h-5 w-5" /></a>
            </Button>
            <div>
              <h1 className="text-2xl md:text-3xl font-bold text-foreground">Character Explains Editor</h1>
              <p className="text-muted-foreground mt-1">Craft a dialogue video with your selected characters.</p>
            </div>
          </div>
          <div className="flex items-center space-x-3">
            <Button variant="outline" onClick={handleSave} disabled={isSaving || isSubmitting}>
              {isSaving ? <Loader2 className="h-4 w-4 mr-2 animate-spin" /> : <Save className="h-4 w-4 mr-2" />}
              Save Draft
            </Button>
            <Button onClick={handleSubmit} disabled={isSubmitting || isSaving}>
              {isSubmitting ? <Loader2 className="h-4 w-4 mr-2 animate-spin" /> : <Send className="h-4 w-4 mr-2" />}
              Submit for Generation
            </Button>
          </div>
        </div>

        {/* Validation Errors */}
        {Object.keys(errors).length > 0 && (
          <Alert variant="destructive">
            <AlertCircle className="h-4 w-4" />
            <AlertDescription>
                <div className="font-semibold mb-2">Please fix the following issues:</div>
                <ul className="list-disc list-inside space-y-1">
                    {Object.values(errors).filter(v => v).map((errorMsg, index) => (
                        <li key={index}>{errorMsg}</li>
                    ))}
                </ul>
            </AlertDescription>
          </Alert>
        )}

        <div className="grid lg:grid-cols-2 gap-8 items-start">
          {/* Left Column: Setup & Customization */}
          <div className="space-y-6">
            <Card className={errors.characterPresetId || errors.topicTitle ? 'border-destructive' : ''}>
              <CardHeader>
                <CardTitle className="flex items-center"><Users className="h-5 w-5 mr-2" />Video Setup</CardTitle>
                <CardDescription>Choose your characters and give your video a title.</CardDescription>
              </CardHeader>
              <CardContent className="space-y-6">
                <div>
                  <Label className="font-semibold">Character Preset *</Label>
                  <div className="grid grid-cols-1 gap-3 mt-2">
                    {PRESETS.map(preset => (
                      <div key={preset.id} onClick={() => handlePresetSelect(preset)}
                        className={`relative cursor-pointer rounded-lg border-2 p-3 transition-all ${params.characterPresetId === preset.id ? 'border-primary bg-primary/5' : 'border-border hover:border-primary/50'}`}>
                        <div className="flex items-center space-x-4">
                          <div className="flex-1 flex items-center justify-between">
                            <div className="flex items-center space-x-3">
                                <h3 className="font-medium text-foreground">{preset.name}</h3>
                                <div className="flex -space-x-2">
                                    {preset.characters.map(char => (
                                        <Image
                                            key={char.id}
                                            src={char.avatar}
                                            alt={char.name}
                                            width={48}
                                            height={48}
                                            className="w-12 h-12 rounded-full border-2 border-background"
                                        />
                                    ))}
                                </div>
                            </div>
                          </div>
                          {params.characterPresetId === preset.id && <Badge>Selected</Badge>}
                        </div>
                      </div>
                    ))}
                  </div>
                  {errors.characterPresetId && <p className="text-sm text-destructive mt-2">{errors.characterPresetId}</p>}
                </div>
                <div className="space-y-2">
                  <Label htmlFor="topic-title" className="font-semibold">Topic Title *</Label>
                  <Input id="topic-title" placeholder="e.g., Why Pineapple on Pizza is Great"
                    value={params.topicTitle} onChange={(e) => handleFormChange('topicTitle', e.target.value)}
                    className={errors.topicTitle ? 'border-destructive' : ''} />
                  {errors.topicTitle && <p className="text-sm text-destructive">{errors.topicTitle}</p>}
                </div>
              </CardContent>
            </Card>

            <VideoCustomization>
              <VideoCustomization.BackgroundVideo
                  value={params.backgroundVideoId}
                  onChange={(value) => handleFormChange('backgroundVideoId', value)}
                  error={errors.backgroundVideoId}
              />
            </VideoCustomization>

            <SubtitleOptions
                value={params.subtitles}
                onChange={handleSubtitleChange}
                hasErrors={!!errors.subtitles}
            />

          </div>


          {/* Right Column: Dialogue Scripting */}
          <div className="space-y-6 lg:sticky top-8">
            <Card className={errors.dialogue ? 'border-destructive' : ''}>
              <CardHeader>
                <CardTitle className="flex items-center"><MessageSquare className="h-5 w-5 mr-2" />Dialogue Script *</CardTitle>
                <CardDescription>
                  Write the conversation. Press Enter to add a line, or Shift+Enter for a new line in the text.
                  {errors.dialogue && <p className="text-destructive font-medium mt-1">{errors.dialogue}</p>}
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div ref={dialogueScrollRef} className="h-80 overflow-y-auto border rounded-lg p-3 bg-muted/30 space-y-4">
                  {params.dialogue.length === 0 ? (
                    <div className="text-center text-muted-foreground py-12">
                      <MessageSquare className="h-12 w-12 mx-auto mb-4 opacity-50" />
                      <p>{activePreset ? 'Start writing your script below!' : 'Select a character preset to begin.'}</p>
                    </div>
                  ) : (
                    params.dialogue.map((line, index) => {
                      const character = getCharacterById(line.characterId);
                      return (
                        <div key={index} className="flex items-start space-x-3 group">
                          <Image src={character?.avatar || ''} alt={character?.name || ''} width={32} height={32} className="w-8 h-8 rounded-full flex-shrink-0 mt-1" />
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center justify-between">
                              <span className="text-sm font-bold" style={{ color: character?.color }}>{character?.name}</span>
                               <Button variant="ghost" size="sm" onClick={() => removeDialogueLine(index)} className="opacity-0 group-hover:opacity-100 transition-opacity h-6 w-6 p-0 text-destructive">
                                <Trash2 className="h-3.5 w-3.5" />
                              </Button>
                            </div>
                            <p className="text-sm text-foreground whitespace-pre-wrap">{line.text}</p>
                          </div>
                        </div>
                      );
                    })
                  )}
                </div>

                {activePreset && (
                  <div className="mt-4 pt-4 border-t space-y-4">
                    <div className="grid grid-cols-2 gap-4 items-end">
                       <div className="space-y-2">
                         <Label className="text-sm font-medium">Speaker</Label>
                         <Select value={activeSpeakerId} onValueChange={setActiveSpeakerId}>
                           <SelectTrigger>
                             <SelectValue placeholder="Select a speaker" />
                           </SelectTrigger>
                           <SelectContent>
                             {activePreset.characters.map(char => (
                               <SelectItem key={char.id} value={char.id}>
                                 <div className="flex items-center space-x-2">
                                   <Image src={char.avatar} alt={char.name} width={20} height={20} className="w-5 h-5 rounded-full" />
                                   <span>{char.name}</span>
                                 </div>
                               </SelectItem>
                             ))}
                           </SelectContent>
                         </Select>
                       </div>
                       <Button onClick={addDialogueLine} disabled={!newDialogueText.trim() || !activeSpeakerId}>
                         <Plus className="h-4 w-4 mr-2" /> Add Dialogue
                       </Button>
                    </div>
                     <div className="space-y-2">
                        <Label htmlFor="dialogue-text">Dialogue Text</Label>
                        <Textarea ref={textareaRef} id="dialogue-text" placeholder={`Type what ${getCharacterById(activeSpeakerId)?.name || '...'} says`}
                          value={newDialogueText} onChange={(e) => setNewDialogueText(e.target.value)} onKeyDown={handleKeyDown} rows={3} className="resize-none" />
                      </div>
                  </div>
                )}
              </CardContent>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
}
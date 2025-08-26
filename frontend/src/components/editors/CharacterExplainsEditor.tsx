'use client';

import { useState, useMemo, useEffect, useRef, forwardRef, useImperativeHandle } from 'react';
import { CharacterExplainsCreationPayload, CharacterExplainsDraft, CharacterExplainsParams } from '@/types'; // Assuming types are in @/types
import {
  Plus, Trash2, AlertCircle, Users, MessageSquare, Loader2, Film
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
import { CharacterPreset } from '@/types';
import apiClient from '@/lib/apiClient';
import { EditorHandle } from '@/types/editor';
import { calculateApproximatePrice } from '@/lib/pricingUtils';
import { FormControl } from '../ui/FormControl';

interface DialogueLine {
  characterId: string;
  text: string;
}

// Create a union type for the data the editor can receive
type CharacterExplainsEditorData = CharacterExplainsDraft | CharacterExplainsCreationPayload;

// --- Prop Definition ---
interface EditorProps {
  initialData: CharacterExplainsEditorData;
  onDirtyChange: (isDirty: boolean) => void;
  onPriceUpdate: (priceInCents: number) => void;
}

// --- Default values for a new draft ---
const defaultParams: CharacterExplainsParams = {
  characterPresetId: undefined,
  topicTitle: '',
  dialogue: [],
  backgroundVideoId: 'minecraft1',
  backgroundMusicId: 'none',
  aspectRatio: '9:16',
  subtitles: { show: true, color: '#FFFFFF', font: 'Arial', position: 'center' },
};

// --- Validation Logic ---
const validate = (params: CharacterExplainsParams): Record<string, string> => {
  const errors: Record<string, string> = {};
  if (!params.characterPresetId) errors.characterPresetId = 'A character preset must be selected.';
  if (!params.topicTitle?.trim() || params.topicTitle.length < 3) errors.topicTitle = 'Topic Title must be at least 3 characters.';
  if (params.dialogue?.length === 0) errors.dialogue = 'Dialogue script cannot be empty.';
  if (!params.backgroundVideoId) errors.backgroundVideoId = 'A background video must be selected.';
  return errors;
};

// --- Main Editor Component ---
export const CharacterExplainsEditor = forwardRef<EditorHandle, EditorProps>(
  ({ initialData, onDirtyChange, onPriceUpdate }: EditorProps, ref) => {
  const [presets, setPresets] = useState<CharacterPreset[]>([]);
  const [presetsLoading, setPresetsLoading] = useState(true);
  const [params, setParams] = useState<CharacterExplainsParams>({
    ...defaultParams,
    ...initialData.templateParams,
    subtitles: { ...defaultParams.subtitles, ...initialData.templateParams?.subtitles },
  });

  const [errors, setErrors] = useState<Record<string, string>>({});
  
  // Local state for the dialogue input form
  const [newDialogueText, setNewDialogueText] = useState('');
  const [activeSpeakerId, setActiveSpeakerId] = useState<string>('');

  const dialogueScrollRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // Memoize the active preset and characters for efficiency
  const activePreset = useMemo(() => presets.find(p => p.presetId === params.characterPresetId), [params.characterPresetId, presets]);

  // --- Effects ---

  // Effect to check for changes and report dirty status to parent
  useEffect(() => {
    // Simple stringify comparison is sufficient for this use case
    const hasChanged = JSON.stringify(initialData.templateParams) !== JSON.stringify(params);
    onDirtyChange(hasChanged);
  }, [params, initialData.templateParams, onDirtyChange]);
  
  useEffect(() => {
    const totalChars = params.dialogue?.reduce((sum, entry) => sum + (entry.text?.length || 0), 0) || 0;
    const priceInCents = calculateApproximatePrice(totalChars);
    onPriceUpdate(priceInCents);
  }, [params, onPriceUpdate]);

  // Expose a function to the parent via the ref
  useImperativeHandle(ref, () => ({
    getValidatedData: () => {
      const validationErrors = validate(params);
      setErrors(validationErrors);
      if (Object.keys(validationErrors).length > 0) {
        window.scrollTo({ top: 0, behavior: 'smooth' });
        return null; // Indicate validation failure
      }
      return params; // Return valid data
    },
  }));

  // --- FETCH PRESETS ---
  useEffect(() => {
    async function fetchPresets() {
      try {
        const response = await apiClient.get<CharacterPreset[]>('/presets/characters');
        setPresets(response.data);
        console.log("Fetched presets:", response.data);
      } catch (error) {
        console.error(error);
      } finally {
        setPresetsLoading(false);
      }
    }
    fetchPresets();
  }, []);

  // Scroll dialogue to bottom on new line
  useEffect(() => {
    if (dialogueScrollRef.current) {
      dialogueScrollRef.current.scrollTop = dialogueScrollRef.current.scrollHeight;
    }
  }, [params.dialogue]);

  // Set the default active speaker when the preset changes
  useEffect(() => {
    if (activePreset && activePreset.characters.length > 0) {
      if (!activeSpeakerId || !activePreset.characters.some(c => c.characterId === activeSpeakerId)) {
        setActiveSpeakerId(activePreset.characters[0].characterId);
      }
    } else {
      setActiveSpeakerId('');
    }
  }, [activePreset, activeSpeakerId]);

  // --- Handlers ---
  
  const handlePresetSelect = (preset: CharacterPreset) => {
    setParams(p => ({ ...p, characterPresetId: preset.presetId, dialogue: [] })); // Reset dialogue on preset change
    if (errors.characterPresetId) {
        setErrors(currentErrors => {
            // eslint-disable-next-line @typescript-eslint/no-unused-vars
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
            // eslint-disable-next-line @typescript-eslint/no-unused-vars
            const { dialogue: _dialogue, ...rest } = currentErrors;
            return rest;
        });
    }

    // Auto-alternate speaker
    if (activePreset) {
      const currentIndex = activePreset.characters.findIndex(c => c.characterId === activeSpeakerId);
      const nextIndex = (currentIndex + 1) % activePreset.characters.length;
      setActiveSpeakerId(activePreset.characters[nextIndex].characterId);
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

  const handleFormChange = (field: keyof CharacterExplainsParams, value: unknown) => {
    setParams(p => ({ ...p, [field]: value }));
    if (errors[field]) {
      setErrors(currentErrors => {
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        const { [field]: _removedError, ...rest } = currentErrors;
        return rest;
      });
    }
  };

  const handleSubtitleChange = (field: keyof NonNullable<CharacterExplainsParams['subtitles']>, value: unknown) => {
    setParams(p => ({ ...p, subtitles: { ...p.subtitles, [field]: value } }));
  };

  const getCharacterById = (id: string) => activePreset?.characters.find(c => c.characterId === id);

  // --- RENDER A LOADING STATE ---
  if (presetsLoading) {
    return (
        <div className="flex justify-center items-center h-screen">
            <Loader2 className="h-8 w-8 animate-spin" />
            <span className="ml-4 text-xl">Loading Editor...</span>
        </div>
    );
  }

  return (
    <div className="bg-background min-h-screen p-4 md:p-6 lg:p-8">
      <div className="max-w-7xl mx-auto space-y-8">

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
                    {presets.map(preset => (
                      <div key={preset.id} onClick={() => handlePresetSelect(preset)}
                        className={`relative cursor-pointer rounded-lg border-2 p-3 transition-all ${params.characterPresetId === preset.presetId ? 'border-primary bg-primary/5' : 'border-border hover:border-primary/50'}`}>
                        <div className="flex items-center space-x-4">
                          <div className="flex-1 flex items-center justify-between">
                            <div className="flex items-center space-x-3">
                                <h3 className="font-medium text-foreground">{preset.name}</h3>
                                <div className="flex -space-x-2">
                                    {preset.characters.map(char => (
                                        <Image
                                            key={char.characterId}
                                            src={char.avatarUrl}
                                            alt={char.name}
                                            width={48}
                                            height={48}
                                            className="w-12 h-12 rounded-full border-2 border-background"
                                        />
                                    ))}
                                </div>
                            </div>
                          </div>
                          {params.characterPresetId === preset.presetId && <Badge>Selected</Badge>}
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
              <FormControl
                label="Background Video"
                required
                icon={Film}
                error={errors.backgroundVideoId as string | undefined}
              >
                <VideoCustomization.BackgroundVideo
                    value={params.backgroundVideoId}
                    onChange={(value) => handleFormChange('backgroundVideoId', value)}
                />
              </FormControl>
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
                          <Image src={character?.avatarUrl || ''} alt={character?.name || ''} width={32} height={32} className="w-8 h-8 rounded-full flex-shrink-0 mt-1" />
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center justify-between">
                              <span className="text-sm font-bold" >{character?.name}</span>
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
                               <SelectItem key={char.characterId} value={char.characterId}>
                                 <div className="flex items-center space-x-2">
                                   <Image src={char.avatarUrl} alt={char.name} width={20} height={20} className="w-5 h-5 rounded-full" />
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
);

CharacterExplainsEditor.displayName = "CharacterExplainsEditor";
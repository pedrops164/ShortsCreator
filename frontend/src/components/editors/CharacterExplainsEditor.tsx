'use client';

import { useState, ChangeEvent, useMemo, useEffect, useRef } from 'react';
import { CharacterExplainsDraft, CharacterExplainsParams } from '@/types';
import { PlusCircle, Send, Trash2, AlertCircle } from 'lucide-react';
import { FormField, FormSection } from '@/components/editors/customization/common';
import Image from 'next/image'; // For displaying character images
import { SubtitleOptions } from './customization/SubtitleOptions';
import { VideoOptions } from './customization/VideoOptions';

// --- Type Definitions ---
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
}

// --- Default values for a new draft ---
const defaultParams: CharacterExplainsParams = {
  characterPresetId: 'peter_stewie',
  topicTitle: '',
  dialogue: [],
  backgroundVideoId: 'minecraft1',
  aspectRatio: '9:16',
  subtitles: {
    show: true,
    color: '#FFFFFF',
    font: 'Arial',
    position: 'bottom',
  },
};

const PRESETS = [
    { id: 'peter_stewie', name: 'Peter & Stewie Griffin', images: ['/character_images/peter.png', '/character_images/stewie.png'] },
    { id: 'rick_morty', name: 'Rick Sanchez & Morty Smith', images: ['/character_images/rick.png', '/character_images/morty.png'] },
];

// --- Validation Logic ---
const validate = (params: CharacterExplainsParams): Record<string, any> => {
  const errors: Record<string, any> = {};

  if (!params.topicTitle?.trim()) errors.topicTitle = 'Topic Title is required.';
  else if (params.topicTitle.length < 3) errors.topicTitle = 'Topic Title must be at least 3 characters.';

  if (!params.characterPresetId) errors.characterPresetId = 'A character preset must be selected.';
  if (params.dialogue.length === 0) errors.dialogue = 'Dialogue cannot be empty.';
  if (!params.backgroundVideoId) errors.backgroundVideoId = 'Background video is required.';

  // Nested validation for subtitles
  if (params.subtitles?.show) {
    if (!params.subtitles.font?.trim()) {
      errors.subtitles = { ...errors.subtitles, font: 'Subtitle font is required.' };
    }
    if (!/^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$/.test(params.subtitles.color)) {
      errors.subtitles = { ...errors.subtitles, color: 'Must be a valid hex color.' };
    }
  }

  return errors;
};


// --- Main Editor Component ---
export function CharacterExplainsEditor({ initialData, onSave, onSubmit, isSaving }: EditorProps) {
  const [params, setParams] = useState<CharacterExplainsParams>({
    ...defaultParams,
    ...initialData.templateParams,
    subtitles: { // Ensure subtitles object is fully formed
      ...defaultParams.subtitles,
      ...initialData.templateParams?.subtitles,
    }
  });
  const [errors, setErrors] = useState<Record<string, any>>({});

  const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setParams(prev => ({ ...prev, [name]: value }));
  };
  
  // Handler for nested subtitle changes
  const handleSubtitleChange = (e: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value, type } = e.target;
    const isCheckbox = type === 'checkbox';
    const checkedValue = isCheckbox ? (e.target as HTMLInputElement).checked : null;
    
    setParams(prev => ({
      ...prev,
      subtitles: {
        ...prev.subtitles,
        [name]: isCheckbox ? checkedValue : value,
      }
    }));
  };

  const handleDialogueChange = (newDialogue: DialogueLine[]) => {
    setParams(prev => ({ ...prev, dialogue: newDialogue }));
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
      console.log('Validation failed:', validationErrors);
    }
  };

  const speakers = useMemo(() => {
      if (!params.characterPresetId) return [];
      const preset = PRESETS.find(p => p.id === params.characterPresetId);
      if (!preset) return [];
      
      // Create a more detailed speakers object array
      return preset.id.split('_').map((id, index) => ({
        id: id,
        name: preset.name, // Keep name for alt text
        image: preset.images[index]
      }));
  }, [params.characterPresetId]);

  return (
    <div className="max-w-4xl mx-auto space-y-8">
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-2xl font-bold text-primary">Character Explains Editor</h2>
          <p className="text-accent mt-1">Write a script for your characters to perform.</p>
        </div>
        <div className="flex items-center gap-4">
          <button onClick={handleSave} disabled={isSaving} className="bg-accent/20 text-foreground font-semibold py-2 px-4 rounded-lg hover:bg-accent/40 transition-colors disabled:opacity-50">
            {isSaving ? 'Saving...' : 'Save Draft'}
          </button>
          <button onClick={handleSubmit} className="flex items-center gap-2 bg-primary text-white font-bold py-2 px-4 rounded-lg hover:bg-primary/90 transition-colors">
            <Send size={16} />
            Submit for Generation
          </button>
        </div>
      </div>
      
      {Object.keys(errors).length > 0 && (
         <div className="p-4 bg-red-500/10 border border-red-500/30 rounded-lg text-red-400">
          <div className="flex items-center gap-2 font-semibold"><AlertCircle size={20} />Please fix the following errors:</div>
          <ul className="list-disc pl-10 mt-2 text-sm">
            {Object.entries(errors).map(([key, value]) => (
                typeof value === 'string' ? <li key={key}>{value}</li> :
                Object.values(value).map((err, i) => <li key={`${key}-${i}`}>{err as string}</li>)
            ))}
          </ul>
        </div>
      )}

      <FormSection title="Video Topic">
        <CharacterPresetSelector selectedId={params.characterPresetId} onSelect={id => setParams(p => ({ ...p, characterPresetId: id, dialogue: [] }))} />
        {errors.characterPresetId && <p className="text-red-500 text-xs mt-1">{errors.characterPresetId}</p>}
        <FormField label="Topic Title" name="topicTitle" value={params.topicTitle} onChange={handleChange} error={errors.topicTitle} />
      </FormSection>

      <FormSection title="Dialogue Script">
        <DialogueManager 
            initialDialogue={params.dialogue} 
            onUpdate={handleDialogueChange} 
            speakers={speakers}
            error={errors.dialogue}
        />
      </FormSection>

      {/* Video customization */}
      <VideoOptions>
        <VideoOptions.BackgroundVideo 
          value={params.backgroundVideoId} 
          onChange={handleChange} 
          error={errors.backgroundVideoId} 
        />
      </VideoOptions>

      {/* Subtitle component */}
      <SubtitleOptions params={params.subtitles} errors={errors.subtitles} onChange={handleSubtitleChange} />
      
    </div>
  );
}


// Template-Specific Components

function CharacterPresetSelector({ selectedId, onSelect }: { selectedId: string; onSelect: (id: string) => void }) {
    return (
      <div>
        <label className="block text-sm font-medium text-foreground/80 mb-2">Character Preset</label>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {PRESETS.map(preset => (
                <div key={preset.id} onClick={() => onSelect(preset.id)}
                    className={`p-4 border-2 rounded-lg cursor-pointer transition-all ${selectedId === preset.id ? 'border-primary shadow-lg' : 'border-accent/50 hover:border-primary/50'}`}>
                    <div className="flex items-center gap-4">
                        <div className="flex -space-x-4">
                            {preset.images.map(src => (
                                <Image key={src} src={src} alt={preset.name} width={48} height={48} className="w-12 h-12 rounded-full border-2 border-background" />
                            ))}
                        </div>
                        <span className="font-semibold text-foreground">{preset.name}</span>
                    </div>
                </div>
            ))}
        </div>
      </div>
    );
}

function DialogueManager({ initialDialogue, onUpdate, speakers, error }: {
  initialDialogue: DialogueLine[];
  onUpdate: (dialogue: DialogueLine[]) => void;
  speakers: { id: string; name: string; image: string; }[];
  error?: string
}) {
  const [newLine, setNewLine] = useState({ characterId: speakers[0]?.id || '', text: '' });

  // Ref for the scrollable container. This is used to scroll to the bottom when a new line is added.
  const dialogueContainerRef = useRef<HTMLDivElement>(null);
  
  // When the speakers change, reset the selected character in the 'add new' form.
  useEffect(() => {
    setNewLine(p => ({ ...p, characterId: speakers[0]?.id || '' }));
  }, [speakers]);

  // useEffect hook to scroll down when dialogue changes
  useEffect(() => {
    if (dialogueContainerRef.current) {
      const { scrollHeight } = dialogueContainerRef.current;
      dialogueContainerRef.current.scrollTo({ top: scrollHeight, behavior: 'smooth' });
    }
  }, [initialDialogue]);

  const addLine = () => {
    if (newLine.text.trim() === '') return;
    onUpdate([...initialDialogue, newLine]);

    // Automatic Speaker Switching Logic
    // Find the other speaker to alternate speakers.
    const otherSpeaker = speakers.find(s => s.id !== newLine.characterId);

    // Reset the form, switching to the other speaker if found.
    setNewLine({
      characterId: otherSpeaker?.id || speakers[0]?.id || '',
      text: ''
    });
  };

  const removeLine = (indexToRemove: number) => {
    onUpdate(initialDialogue.filter((_, index) => index !== indexToRemove));
  };
  
  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      addLine();
    }
  };

  return (
    <div className="space-y-2">
      {error && <p className="text-red-500 text-sm">{error}</p>}
      <div ref={dialogueContainerRef} className="space-y-2 max-h-96 overflow-y-auto pr-2">
        {initialDialogue.map((line, index) => {
          const speaker = speakers.find(s => s.id === line.characterId);
          return (
            <div key={index} className="flex items-start gap-3 p-2 rounded bg-background/50">
              {/* Show speaker image */}
              {speaker && <Image src={speaker.image} alt={speaker.id} width={32} height={32} className="w-8 h-8 rounded-full border-2 border-accent" />}
              <div className="flex-1">
                <p className="whitespace-pre-wrap text-sm text-accent">{line.text}</p>
              </div>
              <button onClick={() => removeLine(index)} className="p-1 text-red-500 hover:text-red-400">
                <Trash2 size={16} />
              </button>
            </div>
          );
        })}
      </div>
      <div className="flex items-end gap-2 p-2 border-t border-accent/30 pt-4">
        <div>
          <label className="mb-2 block text-sm font-medium text-foreground/80">Speaker</label>
          <div className="flex items-center gap-2">
            {speakers.map(speaker => (
              <button key={speaker.id} onClick={() => setNewLine(p => ({...p, characterId: speaker.id}))}
                      className={`rounded-full transition-all ring-offset-background ring-offset-2 ${newLine.characterId === speaker.id ? 'ring-2 ring-primary' : 'ring-0 hover:ring-2 ring-primary/50'}`}>
                <Image src={speaker.image} alt={speaker.id} width={40} height={40} className="w-10 h-10 rounded-full" />
              </button>
            ))}
          </div>
        </div>
        <div className="flex-1">
            <label className="block text-sm font-medium text-foreground/80 mb-1">Dialogue Text (Shift+Enter for new line)</label>
            <textarea value={newLine.text} onChange={e => setNewLine(p => ({...p, text: e.target.value}))} onKeyDown={handleKeyDown}
                    className="block w-full bg-background/50 border rounded-md p-2 focus:ring-primary/50 border-accent" rows={2}/>
        </div>
        <button onClick={addLine} className="bg-accent/30 hover:bg-accent/50 text-foreground p-2 rounded-md h-[42px]">
          <PlusCircle size={20} />
        </button>
      </div>
    </div>
  );
}
'use client';

import { useState, ChangeEvent } from 'react';
import { RedditStoryDraft, RedditStoryParams } from '@/types';
import { PlusCircle, Send, Trash2, AlertCircle } from 'lucide-react';
import { FormField, FormSection, FormSelect } from '@/components/editors/customization/common';
import { SubtitleOptions } from './customization/SubtitleOptions';
import { VideoOptions } from './customization/VideoOptions';

// --- Prop Definition ---
interface EditorProps {
  initialData: RedditStoryDraft;
  onSave: (data: RedditStoryDraft) => void;
  onSubmit: (data: RedditStoryDraft) => void;
  isSaving: boolean;
}

interface Comment {
  author: string;
  text: string;
}

// --- Default values for a new draft ---
const defaultParams: RedditStoryParams = {
  username: '',
  subreddit: '',
  postTitle: '',
  postDescription: '',
  comments: [], // Default to an empty array
  backgroundVideoId: 'minecraft1',
  backgroundMusicId: '',
  avatarImageUrl: 'assets/reddit/reddit_avatar_placeholder.png',
  aspectRatio: '9:16',
  subtitles: {
    show: true,
    color: '#FFFFFF',
    font: 'Arial',
    position: 'bottom',
  },
  voiceSelection: 'openai_alloy',
  theme: 'dark',
};

// --- Validation Logic ---
const validate = (params: RedditStoryParams): Record<string, any> => {
  const errors: Record<string, any> = {};

  // Required string fields and minLength checks
  if (!params.username?.trim()) errors.username = 'Username is required.';
  else if (params.username.length < 3) errors.username = 'Username must be at least 3 characters.';

  if (!params.subreddit?.trim()) errors.subreddit = 'Subreddit is required.';

  if (!params.postTitle?.trim()) errors.postTitle = 'Post Title is required.';
  else if (params.postTitle.length < 3) errors.postTitle = 'Post Title must be at least 3 characters.';

  if (!params.postDescription?.trim()) errors.postDescription = 'Post Description is required.';
  else if (params.postDescription.length < 5) errors.postDescription = 'Post Description must be at least 5 characters.';

  if (!params.backgroundVideoId) errors.backgroundVideoId = 'Background video is required.';
  if (!params.voiceSelection) errors.voiceSelection = 'Narration voice is required.';

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
export function RedditStoryEditor({ initialData, onSave, onSubmit, isSaving }: EditorProps) {
  // Merge initial data over the defaults to create a complete state object
  const [params, setParams] = useState<RedditStoryParams>({
    ...defaultParams,
    ...initialData.templateParams,
  });
  const [errors, setErrors] = useState<Record<string, string>>({});

  const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    const { name, value, type } = e.target;
    
    const isCheckbox = type === 'checkbox';
    // Asserting the target is a checkbox to access the 'checked' property
    const checkedValue = isCheckbox ? (e.target as HTMLInputElement).checked : null;

    setParams(prev => ({
      ...prev,
      [name]: isCheckbox ? checkedValue : value,
      subtitles: {
        ...prev.subtitles,
        [name]: isCheckbox ? checkedValue : value,
      }
    }));
  };

  // Handler for updating the comments array from the sub-component
  const handleCommentsChange = (newComments: Comment[]) => {
    setParams(prev => ({ ...prev, comments: newComments }));
  };

  // Handler for the final save action
  const handleSave = () => {
    // Merge the updated params back into the main draft object before saving
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
  }

  return (
    <div className="max-w-4xl mx-auto space-y-8">
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-2xl font-bold text-primary">Reddit Story Editor</h2>
          <p className="text-accent mt-1">Fill out the details to create your video.</p>
        </div>
        <div className="flex items-center gap-4">
          <button
            onClick={handleSave}
            disabled={isSaving}
            className="bg-accent/20 text-foreground font-semibold py-2 px-4 rounded-lg hover:bg-accent/40 transition-colors disabled:opacity-50"
          >
            {isSaving ? 'Saving...' : 'Save Draft'}
          </button>
          <button
            onClick={handleSubmit}
            className="flex items-center gap-2 bg-primary text-white font-bold py-2 px-4 rounded-lg hover:bg-primary/90 transition-colors"
          >
            <Send size={16} />
            Submit for Generation
          </button>
        </div>
      </div>

      {/* --- Validation Error Summary --- */}
      {Object.keys(errors).length > 0 && (
        <div className="p-4 bg-red-500/10 border border-red-500/30 rounded-lg text-red-400">
          <div className="flex items-center gap-2 font-semibold">
            <AlertCircle size={20} />
            Please fix the following errors:
          </div>
          <ul className="list-disc pl-10 mt-2 text-sm">
            {Object.values(errors).map((error, i) => <li key={i}>{error}</li>)}
          </ul>
        </div>
      )}

      {/* --- Main Post Content Section --- */}
      <FormSection title="Post Content">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <FormField label="Username" name="username" value={params.username} onChange={handleChange} error={errors.username} />
          <FormField label="Subreddit" name="subreddit" value={params.subreddit} onChange={handleChange} placeholder="e.g., AskReddit" error={errors.subreddit} />
        </div>
        <FormField label="Post Title" name="postTitle" value={params.postTitle} onChange={handleChange} error={errors.postTitle} />
        <FormField
          label="Post Description"
          name="postDescription"
          value={params.postDescription}
          onChange={handleChange}
          as="textarea"
          rows={5}
          error={errors.postDescription}
        />
      </FormSection>

      {/* --- Comments Section --- */}
      <FormSection title="Comments">
        <CommentsManager initialComments={params.comments} onUpdate={handleCommentsChange} />
      </FormSection>

      {/* --- Customization Section --- */}
      {/* <FormSection title="Video Customization">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <FormSelect label="Background Video" name="backgroundVideoId" value={params.backgroundVideoId} onChange={handleChange} error={errors.backgroundVideoId}>
            <option value="minecraft1">Minecraft Parkour</option>
            <option value="gta1">GTA Gameplay</option>
          </FormSelect>
          <FormSelect label="Background Music" name="backgroundMusicId" value={params.backgroundMusicId} onChange={handleChange}>
            <option value="">None</option>
            <option value="fun_1">Fun & Upbeat</option>
            <option value="mysterious_1">Mysterious Vibe</option>
          </FormSelect>
          <FormSelect label="Narration Voice" name="voiceSelection" value={params.voiceSelection} onChange={handleChange} error={errors.voiceSelection}>
            <option value="openai_alloy">Alloy (Neutral)</option>
            <option value="openai_ash">Ash (Male)</option>
            <option value="openai_ballad">Ballad (Female)</option>
            <option value="openai_coral">Coral (Male)</option>
            <option value="openai_echo">Echo (Female)</option>
            <option value="openai_fable">Fable (Female)</option>
            <option value="openai_onyx">Onyx (Male)</option>
            <option value="openai_nova">Nova (Female)</option>
            <option value="openai_sage">Sage (Male)</option>
            <option value="openai_shimmer">Shimmer (Female)</option>
            <option value="openai_verse">Verse (Neutral)</option>
          </FormSelect>
          <FormSelect label="Reddit Theme" name="theme" value={params.theme} onChange={handleChange}>
            <option value="dark">Dark</option>
            <option value="light">Light</option>
          </FormSelect>
        </div>
      </FormSection> */}
      <VideoOptions>
        <VideoOptions.BackgroundVideo
          value={params.backgroundVideoId}
          onChange={handleChange}
          error={errors.backgroundVideoId}
        />
        <VideoOptions.Music
          value={params.backgroundMusicId}
          onChange={handleChange}
        />
        <VideoOptions.Voice 
          value={params.voiceSelection}
          onChange={handleChange}
          error={errors.voiceSelection}
        />
        <VideoOptions.Theme
          value={params.theme}
          onChange={handleChange}
        />
      </VideoOptions>

      <SubtitleOptions params={params.subtitles} errors={errors.subtitles} onChange={handleChange} />
    </div>
  );
}

// Dedicated component to manage the dynamic list of comments
function CommentsManager({ initialComments, onUpdate }: { initialComments: Comment[]; onUpdate: (comments: Comment[]) => void; }) {
  const [newComment, setNewComment] = useState({ author: '', text: '' });

  const addComment = () => {
    if (newComment.text.trim() === '') return;
    const author = newComment.author.trim() === '' ? 'Commenter' : newComment.author;
    onUpdate([...initialComments, { ...newComment, author }]);
    setNewComment({ author: '', text: '' }); // Reset form
  };

  const removeComment = (indexToRemove: number) => {
    onUpdate(initialComments.filter((_, index) => index !== indexToRemove));
  };

  return (
    <div className="space-y-4">
      {initialComments.map((comment, index) => (
        <div key={index} className="flex items-start gap-2 bg-background/50 p-2 rounded">
          <div className="flex-1">
            <p className="font-semibold text-sm text-foreground">{comment.author}</p>
            <p className="text-sm text-accent">{comment.text}</p>
          </div>
          <button onClick={() => removeComment(index)} className="text-red-500 hover:text-red-400 p-1">
            <Trash2 size={16} />
          </button>
        </div>
      ))}
      <div className="flex items-end gap-2 p-2 border-t border-accent/30 pt-4">
        <FormField label="Author (Optional)" name="author" placeholder="Author" value={newComment.author} onChange={e => setNewComment(p => ({...p, author: e.target.value}))} />
        <FormField label="Comment Text" name="text" placeholder="Add a new comment..." value={newComment.text} onChange={e => setNewComment(p => ({...p, text: e.target.value}))} />
        <button onClick={addComment} className="bg-accent/30 hover:bg-accent/50 text-foreground p-2 rounded-md h-10">
          <PlusCircle size={20} />
        </button>
      </div>
    </div>
  );
}
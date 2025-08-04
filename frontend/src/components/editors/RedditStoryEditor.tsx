'use client';

import React, { useState, ChangeEvent, useRef } from 'react';
import { RedditStoryDraft, RedditStoryParams } from '@/types';

// --- UI Components & Icons ---
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Separator } from "@/components/ui/separator";
import {
  Save, Send, Plus, Trash2, AlertCircle, MessageSquare, User, Video, ArrowLeft, Loader2, Hash, FileText,
} from "lucide-react";
import { VideoCustomization } from './customization/VideoCustomization';
import { SubtitleOptions } from './customization/SubtitleOptions';

interface EditorProps {
  initialData: RedditStoryDraft;
  onSave: (data: RedditStoryDraft) => void;
  onSubmit: (data: RedditStoryDraft) => void;
  isSaving: boolean;
  isSubmitting: boolean; // Added for separate submit state
}

// --- Default Values ---
const defaultParams: RedditStoryParams = {
  username: '',
  subreddit: '',
  postTitle: '',
  postDescription: '',
  comments: [],
  backgroundVideoId: '',
  backgroundMusicId: 'none',
  avatarImageUrl: 'assets/reddit/reddit_avatar_placeholder.png',
  aspectRatio: '9:16',
  subtitles: {
    show: true,
    color: '#FFFFFF',
    font: 'Arial',
    position: 'bottom',
  },
  voiceSelection: '',
  theme: 'dark',
};

// --- Validation Logic ---
const validate = (params: RedditStoryParams): Record<string, any> => {
  const errors: Record<string, any> = {};
  if (!params.username?.trim()) errors.username = 'Username is required.';
  else if (params.username.length < 3) errors.username = 'Username must be at least 3 characters.';
  if (!params.subreddit?.trim()) errors.subreddit = 'Subreddit is required.';
  if (!params.postTitle?.trim()) errors.postTitle = 'Post Title is required.';
  else if (params.postTitle.length < 5) errors.postTitle = 'Post Title must be at least 5 characters.';
  if (!params.postDescription?.trim()) errors.postDescription = 'Post Description is required.';
  else if (params.postDescription.length < 10) errors.postDescription = 'Post Description must be at least 10 characters.';
  if (!params.backgroundVideoId) errors.backgroundVideoId = 'Background video is required.';
  if (!params.voiceSelection) errors.voiceSelection = 'Narration voice is required.';
  if (params.subtitles?.show) {
    if (!params.subtitles.font?.trim()) errors.subtitles = { ...errors.subtitles, font: 'Subtitle font is required.' };
    if (!/^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$/.test(params.subtitles.color || '')) {
        errors.subtitles = { ...errors.subtitles, color: 'Must be a valid hex color.' };
    }
  }
  return errors;
};

export function RedditStoryEditor({ initialData, onSave, onSubmit, isSaving, isSubmitting }: EditorProps) {
  const [params, setParams] = useState<RedditStoryParams>({
    ...defaultParams,
    ...initialData.templateParams,
  });
  const [errors, setErrors] = useState<Record<string, any>>({});

  const [newComment, setNewComment] = useState({ author: '', text: '' });
  const commentsEndRef = useRef<HTMLDivElement>(null);

  // --- Handlers  ---

  const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setParams(prev => ({ ...prev, [name]: value }));
  };
  
  const handleSelectChange = (name: keyof RedditStoryParams, value: string) => {
    setParams(prev => ({ ...prev, [name]: value }));
  };
  
  const handleSubtitleChange = (field: keyof NonNullable<RedditStoryParams['subtitles']>, value: any) => {
    setParams(p => ({ ...p, subtitles: { ...p.subtitles, [field]: value } }));
  };

  const addComment = () => {
    if (newComment.text.trim() === '') return;
    const author = newComment.author.trim() === '' ? 'Commenter' : newComment.author;
    const updatedComments = [...params.comments, { ...newComment, author }];
    setParams(prev => ({ ...prev, comments: updatedComments }));
    setNewComment({ author: '', text: '' });
  };

  const removeComment = (indexToRemove: number) => {
    const updatedComments = params.comments.filter((_, index) => index !== indexToRemove);
    setParams(prev => ({ ...prev, comments: updatedComments }));
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
      console.log('Validation failed:', validationErrors);
    }
  };

  return (
    <div className="p-4 md:p-6 lg:p-8 bg-background min-h-full">
      <div className="max-w-5xl mx-auto space-y-8">
        {/* Header */}
        <div className="flex items-center justify-between">
            <h1 className="text-3xl md:text-4xl font-bold text-foreground">Reddit Story Editor</h1>
            <div className="flex items-center space-x-3">
            <Button variant="outline" onClick={handleSave} disabled={isSaving}>
                {isSaving ? <Loader2 className="h-4 w-4 mr-2 animate-spin" /> : <Save className="h-4 w-4 mr-2" />}
                Save Draft
            </Button>
            <Button onClick={handleSubmit} disabled={isSubmitting}>
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
                <div className="font-medium text-red-700 dark:text-red-400 mb-2">Please fix the following errors:</div>
                <ul className="list-disc list-inside space-y-1 text-sm">
                    {Object.values(errors).flat().map((error: any, index) => (
                        <li key={index}>{typeof error === 'string' ? error : Object.values(error).join(', ')}</li>
                    ))}
                </ul>
            </AlertDescription>
          </Alert>
        )}

        <div className="space-y-8">
          {/* Reddit Post Details */}
          <Card className={errors.username || errors.subreddit || errors.postTitle || errors.postDescription ? "border-destructive" : ""}>
            <CardHeader>
              <CardTitle className="flex items-center"><MessageSquare className="h-5 w-5 mr-2" />Reddit Post Details</CardTitle>
              <CardDescription>Enter the main Reddit post information.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="grid md:grid-cols-2 gap-6">
                <div className="space-y-2">
                  <Label htmlFor="username">Author's Username *</Label>
                  <div className="relative"><User className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
                    <Input id="username" name="username" placeholder="e.g., throwaway123" value={params.username} onChange={handleChange} className={`pl-10 ${errors.username ? "border-destructive" : ""}`}/>
                  </div>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="subreddit">Subreddit Name *</Label>
                   <div className="relative"><Hash className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
                    <Input id="subreddit" name="subreddit" placeholder="e.g., r/AskReddit" value={params.subreddit} onChange={handleChange} className={`pl-10 ${errors.subreddit ? "border-destructive" : ""}`}/>
                  </div>
                </div>
              </div>
              <div className="space-y-2">
                <Label htmlFor="postTitle">Post Title *</Label>
                <Input id="postTitle" name="postTitle" placeholder="e.g., What's the most embarrassing thing that happened to you?" value={params.postTitle} onChange={handleChange} className={errors.postTitle ? "border-destructive" : ""}/>
              </div>
              <div className="space-y-2">
                <Label htmlFor="postDescription">Post Description *</Label>
                <Textarea id="postDescription" name="postDescription" placeholder="Enter the main story content here..." value={params.postDescription} onChange={handleChange} rows={8} className={`resize-none ${errors.postDescription ? "border-destructive" : ""}`}/>
              </div>
            </CardContent>
          </Card>

          {/* Comments Section */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center"><MessageSquare className="h-5 w-5 mr-2" />Comments Section</CardTitle>
              <CardDescription>Add comments that will be narrated after the main post (optional).</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              {params.comments.length > 0 && (
                <div className="space-y-4">
                  <h4 className="font-medium">Comments ({params.comments.length})</h4>
                  <div className="space-y-3 max-h-80 overflow-y-auto p-1">
                    {params.comments.map((comment, index) => (
                      <div key={index} className="flex items-start space-x-3 p-4 bg-muted/50 rounded-lg group">
                        <div className="flex-shrink-0 w-8 h-8 bg-primary rounded-full flex items-center justify-center text-primary-foreground text-sm font-medium">{index + 1}</div>
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center justify-between mb-1">
                            <span className="text-sm font-medium">u/{comment.author}</span>
                            <Button variant="ghost" size="sm" onClick={() => removeComment(index)} className="opacity-0 group-hover:opacity-100 transition-opacity h-6 w-6 p-0 text-destructive">
                                <Trash2 className="h-3 w-3" />
                            </Button>
                          </div>
                          <p className="text-sm text-muted-foreground whitespace-pre-wrap">{comment.text}</p>
                        </div>
                      </div>
                    ))}
                    <div ref={commentsEndRef} />
                  </div>
                </div>
              )}
              <div className="space-y-4 border-t pt-6">
                <h4 className="font-medium">Add New Comment</h4>
                <div className="grid md:grid-cols-4 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="comment-author">Author (optional)</Label>
                    <Input id="comment-author" placeholder="Commenter" value={newComment.author} onChange={(e) => setNewComment(p => ({...p, author: e.target.value}))}/>
                  </div>
                  <div className="md:col-span-3 space-y-2">
                    <Label htmlFor="comment-text">Comment Text</Label>
                    <Textarea id="comment-text" placeholder="Enter the comment text..." value={newComment.text} onChange={(e) => setNewComment(p => ({...p, text: e.target.value}))} rows={3} className="resize-none"/>
                  </div>
                </div>
                <Button onClick={addComment} disabled={!newComment.text.trim()}><Plus className="h-4 w-4 mr-2" />Add Comment</Button>
              </div>
            </CardContent>
          </Card>

          <VideoCustomization>
            <VideoCustomization.BackgroundVideo
              value={params.backgroundVideoId}
              onChange={(value) => handleSelectChange('backgroundVideoId', value)}
              error={errors.backgroundVideoId}
            />
            <Separator />
            <VideoCustomization.Voice
              value={params.voiceSelection}
              onChange={(value) => handleSelectChange('voiceSelection', value)}
              error={errors.voiceSelection}
            />
            <Separator />
            <VideoCustomization.Theme
              value={params.theme}
              onChange={(value) => handleSelectChange('theme', value)}
              error={errors.theme}
            />
          </VideoCustomization>
          
          <SubtitleOptions
            value={params.subtitles}
            onChange={handleSubtitleChange}
            hasErrors={!!errors.subtitles}
          />
        </div>
      </div>
    </div>
  );
}
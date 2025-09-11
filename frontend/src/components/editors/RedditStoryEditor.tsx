'use client';

import React, { useState, ChangeEvent, useRef, useImperativeHandle, forwardRef, useEffect } from 'react';
import { RedditStoryCreationPayload, RedditStoryDraft, RedditStoryParams } from '@/types';
import { EditorHandle } from '@/types/editor';
import { GenerateTextRequest, GeneratedContentResponse } from '@/types/api';
import apiClient from '@/lib/apiClient';
import { toast } from "sonner"; // Using sonner for toast notifications

// --- UI Components & Icons ---
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Separator } from "@/components/ui/separator";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import {
  Plus, Trash2, AlertCircle, MessageSquare, User, Hash, Film, Mic2, Paintbrush, Sparkles, Loader2
} from "lucide-react";
import { VideoCustomization } from './customization/VideoCustomization';
import { SubtitleOptions } from './customization/SubtitleOptions';
import { calculateApproximatePrice } from '@/lib/pricingUtils';
import { FormControl } from '../ui/FormControl';

// Create a union type for the data the editor can receive
type RedditStoryEditorData = RedditStoryDraft | RedditStoryCreationPayload;

interface EditorProps {
  initialData: RedditStoryEditorData;
  onDirtyChange: (isDirty: boolean) => void;
  onPriceUpdate: (priceInCents: number) => void;
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
  aspectRatio: '9:16',
  subtitles: {
    show: true,
    color: '#FFFFFF',
    font: 'Arial',
    position: 'center',
  },
  voiceSelection: 'openai_alloy',
  theme: 'dark',
};

// --- Validation Logic ---
const validate = (params: RedditStoryParams): Record<string, string | Record<string, string>> => {
  const errors: Record<string, string | Record<string, string>> = {};
  if (!params.username?.trim()) errors.username = 'Username is required.';
  else if (params.username.length < 3) errors.username = 'Username must be at least 3 characters.';
  if (!params.subreddit?.trim()) errors.subreddit = 'Subreddit is required.';
  if (!params.postTitle?.trim()) errors.postTitle = 'Post Title is required.';
  else if (params.postTitle.length < 5) errors.postTitle = 'Post Title must be at least 5 characters.';
  if (!params.backgroundVideoId) errors.backgroundVideoId = 'Background video is required.';
  if (!params.voiceSelection) errors.voiceSelection = 'Narration voice is required.';
  if (params.subtitles?.show) {
    if (!params.subtitles.font?.trim()) errors.subtitles = { ...errors.subtitles as object, font: 'Subtitle font is required.' };
    if (!/^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$/.test(params.subtitles.color || '')) {
        errors.subtitles = { ...errors.subtitles as object, color: 'Must be a valid hex color.' };
    }
  }
  return errors;
};

export const RedditStoryEditor = forwardRef<EditorHandle, EditorProps>(
  ({ initialData, onDirtyChange, onPriceUpdate }: EditorProps, ref) => {
  const [params, setParams] = useState<RedditStoryParams>({
    ...defaultParams,
    ...initialData.templateParams,
  });
  const [errors, setErrors] = useState<Record<string, string | Record<string, string>>>({});
  const [newComment, setNewComment] = useState({ author: '', text: '' });
  const commentsEndRef = useRef<HTMLDivElement>(null);

  // State for AI generation loading status
  const [isGeneratingDescription, setIsGeneratingDescription] = useState(false);
  const [isGeneratingComments, setIsGeneratingComments] = useState(false);

  // State for the confirmation dialog
  const [isConfirming, setIsConfirming] = useState(false);
  // This will hold the function to execute upon confirmation
  const [onConfirmAction, setOnConfirmAction] = useState<(() => void) | null>(null);


  // Effect to check for changes and report dirty status to parent
  useEffect(() => {
    // Simple stringify comparison is sufficient for this use case
    const hasChanged = JSON.stringify(initialData.templateParams) !== JSON.stringify(params);
    onDirtyChange(hasChanged);
  }, [params, initialData.templateParams, onDirtyChange]);

    // --- useEffect for real-time price calculation ---
  useEffect(() => {
    let totalChars = 0;
    totalChars += params.postTitle?.length || 0;
    totalChars += params.postDescription?.length || 0;
    params.comments?.forEach(comment => {
      totalChars += comment.text?.length || 0;
    });
    const priceInCents = 5 + calculateApproximatePrice(totalChars);
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

  // --- Handlers  ---

  const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setParams(prev => ({ ...prev, [name]: value }));
  };
  
  const handleSelectChange = (name: keyof RedditStoryParams, value: string) => {
    setParams(prev => ({ ...prev, [name]: value }));
  };
  
  const handleSubtitleChange = (field: keyof NonNullable<RedditStoryParams['subtitles']>, value: unknown) => {
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

    // AI Generation Handlers

  const handleGenerateDescription = async () => {
    setIsGeneratingDescription(true);
    try {
      const request: GenerateTextRequest = {
        generationType: 'REDDIT_POST_DESCRIPTION',
        context: { postTitle: params.postTitle || '' }
      };
      const response = await apiClient.post<GeneratedContentResponse>('/generate/text', request);

      if (response.data.content.text) {
        setParams(prev => ({ ...prev, postDescription: response.data.content.text || '' }));
        toast.success("Post description generated successfully!");
      }
    } catch (error) {
      console.error("Failed to generate description:", error);
      toast.error("Generation failed. Please try again later.");
    } finally {
      setIsGeneratingDescription(false);
    }
  };

  const handleGenerateComments = async () => {
    setIsGeneratingComments(true);
    try {
      const request: GenerateTextRequest = {
        generationType: 'REDDIT_COMMENT',
        context: { 
          postTitle: params.postTitle || ''
        }
      };
      const response = await apiClient.post<GeneratedContentResponse>('/generate/text', request);
      
      if (response.data.content.comments && response.data.content.comments.length > 0) {
        const newComments = response.data.content.comments.map((text: string) => ({
          author: `Commenter #${Math.floor(Math.random() * 1000)}`, // Randomize author
          text: text,
        }));
        setParams(prev => ({ ...prev, comments: [...prev.comments, ...newComments] }));
        toast.success("Comments generated successfully!");
      } else {
          toast.info("The AI didn't return any comments. Try refining your title.");
      }
    } catch (error) {
      console.error("Failed to generate comments:", error);
      toast.error("Generation failed. Please try again later.");
    } finally {
      setIsGeneratingComments(false);
    }
  };

  // Function to trigger the confirmation dialog
  const requestConfirmation = (action: () => void) => {
    setOnConfirmAction(() => action); // Store the action (e.g., handleGenerateDescription)
    setIsConfirming(true); // Open the dialog
  };

  return (
      <TooltipProvider>
        {/* AlertDialog component for confirmation of text generation charge*/}
        <AlertDialog open={isConfirming} onOpenChange={setIsConfirming}>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>Confirm AI Generation</AlertDialogTitle>
              <AlertDialogDescription>
                This action will use your account balance. The approximate cost for this generation is <strong>$0.01</strong>.
                <br />
                Do you want to proceed?
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel>Cancel</AlertDialogCancel>
              <AlertDialogAction onClick={() => onConfirmAction && onConfirmAction()}>
                Confirm & Generate
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
        <div className="max-w-5xl mx-auto space-y-8">

          {/* Validation Errors */}
          {Object.keys(errors).length > 0 && (
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>
                  <div className="font-medium text-red-700 dark:text-red-400 mb-2">Please fix the following errors:</div>
                  <ul className="list-disc list-inside space-y-1 text-sm">
                      {Object.values(errors)
                        .flatMap(e => (typeof e === 'object' ? Object.values(e) : e))
                        .map((error, index) => (
                          <li key={index}>{String(error)}</li>
                      ))}
                  </ul>
              </AlertDescription>
            </Alert>
          )}

          <div className="space-y-8">
            {/* Reddit Post Details */}
            <Card className={errors.username || errors.subreddit || errors.postTitle ? "border-destructive" : ""}>
              <CardHeader>
                <CardTitle className="flex items-center"><MessageSquare className="h-5 w-5 mr-2" />Reddit Post Details</CardTitle>
                <CardDescription>Enter the main Reddit post information.</CardDescription>
              </CardHeader>
              <CardContent className="space-y-6">
                <div className="grid md:grid-cols-2 gap-6">
                  <div className="space-y-2">
                    <Label htmlFor="username">Author&apos;s Username *</Label>
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
                  <div className="flex justify-between items-center">
                      <Label htmlFor="postDescription">Post Description (optional)</Label>
                      <Tooltip>
                        <TooltipTrigger asChild>
                          {/* The button is wrapped, so we add a span to ensure the tooltip trigger works even when the button is disabled */}
                          <span tabIndex={0}>
                            <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                onClick={() => requestConfirmation(handleGenerateDescription)}
                                disabled={!params.postTitle || isGeneratingDescription}
                            >
                                {isGeneratingDescription ? (
                                    <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                                ) : (
                                    <Sparkles className="h-4 w-4 mr-2" />
                                )}
                                Generate with AI
                            </Button>
                          </span>
                        </TooltipTrigger>
                        {/* Conditionally render content ONLY if the title is the reason for being disabled */}
                        {!params.postTitle && (
                          <TooltipContent>
                            <p>Please enter a Post Title to enable AI generation.</p>
                          </TooltipContent>
                        )}
                      </Tooltip>
                  </div>
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
                  <div className="flex justify-between items-center">
                      <h4 className="font-medium">Add or Generate Comments</h4>
                      <Tooltip>
                         <TooltipTrigger asChild>
                           <span tabIndex={0}>
                            <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                onClick={() => requestConfirmation(handleGenerateComments)}
                                disabled={!params.postTitle || isGeneratingComments}
                            >
                                {isGeneratingComments ? (
                                    <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                                ) : (
                                    <Sparkles className="h-4 w-4 mr-2" />
                                )}
                                Generate Comments with AI
                            </Button>
                           </span>
                         </TooltipTrigger>
                         {!params.postTitle && (
                           <TooltipContent>
                              <p>Please enter a Post Title to enable AI generation.</p>
                           </TooltipContent>
                         )}
                       </Tooltip>
                  </div>

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
              <FormControl
                label="Background Video"
                required
                icon={Film}
                error={errors.backgroundVideoId as string | undefined}
              >
                <VideoCustomization.BackgroundVideo
                  value={params.backgroundVideoId}
                  onChange={(value) => handleSelectChange('backgroundVideoId', value)}
                />
              </FormControl>
              <Separator />
              
              <FormControl
                label="Narration Voice"
                required
                icon={Mic2}
                error={errors.voiceSelection as string | undefined}
              >
                <VideoCustomization.Voice
                  value={params.voiceSelection}
                  onChange={(value) => handleSelectChange('voiceSelection', value)}
                />
              </FormControl>
              <Separator />
              <FormControl
                label="Theme"
                required
                icon={Paintbrush}
                error={errors.theme as string | undefined}
              >
                <VideoCustomization.Theme
                  value={params.theme}
                  onChange={(value) => handleSelectChange('theme', value)}
                />
              </FormControl>
            </VideoCustomization>
            
            <SubtitleOptions
              value={params.subtitles}
              onChange={handleSubtitleChange}
              hasErrors={!!errors.subtitles}
            />
          </div>
        </div>
      </TooltipProvider>
  );
}
);

RedditStoryEditor.displayName = "RedditStoryEditor";
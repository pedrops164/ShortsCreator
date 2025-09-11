'use client';

import { useEffect, useRef, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import apiClient, { ApiError } from '@/lib/apiClient';
import { RedditStoryEditor } from '@/components/editors/RedditStoryEditor';
import { CharacterExplainsEditor } from '@/components/editors/CharacterExplainsEditor'; // Import new editor component
import { Draft } from '@/types/drafts';
import { CreationPayload } from '@/types/creation'; // Import new creation types
import { ContentStatus } from '@/types/content';
import { PriceResponse } from '@/types/balance';
import { EditorHandle } from '@/types/editor';
import { EditorHeader } from './EditorHeader';
import { GenerationConfirmationDialog } from './GenerationConfirmationDialog';

// This map is used to dynamically load the correct editor component based on the template type
const EDITOR_MAP = {
  'reddit_story_v1': { component: RedditStoryEditor, title: 'Reddit Story Editor' },
  'character_explains_v1': { component: CharacterExplainsEditor, title: 'Character Explains Editor' },
};

export default function EditorPage() {
  const router = useRouter();
  const params = useParams(); // Gets route params, e.g., { slug: ['create', 'reddit-story'] }
  const { slug } = params;

  // State to hold draft data. Can be empty for new drafts or populated for existing ones.
  const [draftData, setDraftData] = useState<Draft | CreationPayload | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [isDirty, setIsDirty] = useState(false); // Track if form has changes

  // --- Dialog and Pricing State ---
  const [priceData, setPriceData] = useState<PriceResponse | null>(null);
  const [isDialogOpen, setIsDialogOpen] = useState(false);

  // State to hold the approximate price in cents
  const [approximatePrice, setApproximatePrice] = useState(0);

  // State to hold any generation errors
  const [generationError, setGenerationError] = useState<ApiError | null>(null);
  
  // Ref to communicate with the child editor component
  const editorRef = useRef<EditorHandle>(null);

  useEffect(() => {
    // Logic to decide whether to create a new draft or fetch an existing one
    if (slug && slug[0] === 'create' && slug[1]) {
      // --- CREATE A NEW DRAFT ---
      const templateName = slug[1];
      console.log(`Initializing a new draft for template: ${templateName}`);
      // Set up a new, empty draft object based on the template
      setDraftData({
        templateId: templateName,
        templateParams: {}, // Initialize with empty params, will be filled by the editor
      } as CreationPayload); // Cast to CreationPayload type
      setIsLoading(false);

    } else if (slug && slug[0] === 'edit' && slug[1]) {
      // --- LOAD AN EXISTING DRAFT ---
      const draftId = slug[1];
      console.log(`Fetching existing draft with ID: ${draftId}`);
      const fetchDraft = async () => {
        try {
          const response = await apiClient.get<Draft>(`/content/${draftId}`);
          console.log('Fetched draft data:', response.data);
          setDraftData(response.data);
        } catch (error) {
          console.error('Failed to fetch draft:', error);
          router.replace('/create'); // Redirect on error
        } finally {
          setIsLoading(false);
        }
      };
      fetchDraft();
    } else {
      // Invalid URL, redirect to a safe place
      console.error("Invalid editor URL");
      router.replace('/create');
    }
  }, [slug, router]);

  // This function is passed down to the specific editor component
  const handleSave = async (params: Record<string, unknown>): Promise<Draft | null> => {
    setIsSaving(true);
    try {
      let savedDraft: Draft;
      const isNewDraft = !(draftData && 'id' in draftData && draftData.id);

      if (isNewDraft) {
        const creationPayload = { ...draftData, templateParams: params } as CreationPayload;
        const response = await apiClient.post<Draft>('/content/drafts', creationPayload);
        savedDraft = response.data;
      } else {
        const response = await apiClient.put<Draft>(`/content/${(draftData as Draft).id}`, params);
        savedDraft = response.data;
      }
      
      setDraftData(savedDraft); // Update the state with the saved draft (which now has an ID)
      setIsDirty(false);
      return savedDraft;
    } catch (error) {
      console.error("Failed to save draft:", error);
      return null;
    } finally {
      setIsSaving(false);
    }
  };

  // --- Button Click Handlers ---
  const handleSaveClick = async () => {
    const latestParams = editorRef.current?.getValidatedData();
    if (latestParams) {
      const wasNewDraft = !(draftData && 'id' in draftData && draftData.id);
      const savedDraft = await handleSave(latestParams);

      // If it was a new draft, we need to update the URL
      if (savedDraft && wasNewDraft) {
        router.replace(`/editor/edit/${savedDraft.id}`, { scroll: false });
      }
    }
  };

  const handleGenerateClick = async () => {
    // Get latest data from the editor
    console.log("Generating content with latest params...");
    const latestParams = editorRef.current?.getValidatedData();
    if (!latestParams) {
      console.warn("Validation failed, cannot generate content.");
      return; // Validation failed in child
    }

    console.log("Latest params for generation:", latestParams);
    // Save the draft (even if not dirty, to ensure consistency)
    const savedDraft = await handleSave(latestParams);
    if (!savedDraft?.id) {
      console.error("Save failed, aborting generation.");
      return;
    }

    // Fetch price and open dialog
    try {
      const response = await apiClient.get<PriceResponse>(`/content/${savedDraft.id}/price`);
      setPriceData(response.data);
      setIsDialogOpen(true);
    } catch (error) {
      console.error("Failed to fetch price:", error);
    }
  };

  const handleConfirmGeneration = async () => {
    if (draftData && 'id' in draftData && draftData.id) {
      setGenerationError(null); // Clear previous errors
      try {
        await apiClient.post(`/content/${draftData.id}/generate`);
        router.push('/content');
      } catch (error) {
        console.warn("Failed to submit draft for generation:", error);
        if (error instanceof ApiError) {
          setGenerationError(error); // Set the structured error
        } else {
          // Create a generic error for unexpected issues
          setGenerationError(ApiError.createDefault());
        }
        //setIsDialogOpen(false);
      }
    }
  };

  if (isLoading || !draftData) {
    return <div className="text-center p-12">Loading Editor...</div>;
  }

  // Look up the component from the map using the template name
  const editorInfo = EDITOR_MAP[draftData.templateId as keyof typeof EDITOR_MAP];
  if (!editorInfo) {
    return <div className="text-center p-12 text-red-500">Error: Unknown editor template &quot;{draftData.templateId}&quot;.</div>;
  }

  // Determine if the form should be in a read-only state.
  const isReadOnly = 'status' in draftData && draftData.status === ContentStatus.COMPLETED;

  // Helper function to render the correct editor based on the templateId
  const renderEditor = () => {
    switch (draftData.templateId) {
      case 'reddit_story_v1':
        // Inside this case, TypeScript KNOWS draftData is
        // RedditStoryDraft | RedditStoryCreationPayload.
        // This now perfectly matches the prop type of RedditStoryEditor.
        return (
          <RedditStoryEditor
            ref={editorRef}
            initialData={draftData}
            onDirtyChange={setIsDirty}
            onPriceUpdate={setApproximatePrice}
          />
        );
      
      case 'character_explains_v1':
        // Similarly, TypeScript narrows the type for this case.
        return (
          <CharacterExplainsEditor
            ref={editorRef}
            initialData={draftData}
            onDirtyChange={setIsDirty}
            onPriceUpdate={setApproximatePrice}
          />
        );

      default:
        return <div className="text-red-500">Invalid editor type!</div>;
    }
  };
  
  return (
    <div className="flex flex-col h-screen">
      <EditorHeader
        editorTitle={editorInfo.title}
        onSave={handleSaveClick}
        onGenerate={handleGenerateClick}
        isSaving={isSaving}
        isSaveDisabled={!isDirty || isReadOnly}
        approximatePriceInCents={approximatePrice} // Pass approx price to header to display
      />
      
      <main className="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8">
        <fieldset disabled={isReadOnly || isSaving} className="space-y-8">
          {renderEditor()}
        </fieldset>
      </main>

      <GenerationConfirmationDialog
        isOpen={isDialogOpen}
        onClose={() => {
            setIsDialogOpen(false);
        }}
        onConfirm={handleConfirmGeneration}
        priceInCents={priceData?.finalPrice}
        currency={priceData?.currency}
        error={generationError} // Pass error to dialog
      />
    </div>
  );
}
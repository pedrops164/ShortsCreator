'use client';

import { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import apiClient from '@/lib/apiClient';
import { RedditStoryEditor } from '@/components/editors/RedditStoryEditor';
import { Draft } from '@/types/drafts';
import { CreationPayload } from '@/types/creation'; // Import new creation types

// This map is used to dynamically load the correct editor component based on the template type
const EDITOR_COMPONENT_MAP = {
  'reddit_story_v1': RedditStoryEditor,
};

export default function EditorPage() {
  const router = useRouter();
  const params = useParams(); // Gets route params, e.g., { slug: ['create', 'reddit-story'] }
  const { slug } = params;

  // State to hold draft data. Can be empty for new drafts or populated for existing ones.
  const [draftData, setDraftData] = useState<Draft | CreationPayload | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);

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
  const handleSave = async (updatedData: Draft | CreationPayload): Promise<Draft | null> => {
    setIsSaving(true);
    console.log("Saving data:", updatedData);

    try {
      if ('id' in updatedData && updatedData.id) {
        const response = await apiClient.put<Draft>(`/content/${updatedData.id}`, updatedData.templateParams);
        setDraftData(response.data);
        console.log("Draft updated:", response.data);
        return response.data;
      } else {
        const creationPayload = updatedData as CreationPayload;
        const response = await apiClient.post<Draft>('/content/drafts', creationPayload);
        setDraftData(response.data);
        console.log("New draft created:", response.data);
        router.replace(`/editor/edit/${response.data.id}`);
        return response.data;
      }
    } catch (error) {
      console.error("Failed to save draft:", error);
      return null; // Return null on error
    } finally {
      setIsSaving(false);
    }
  };

  const handleSubmit = async (updatedData: Draft | CreationPayload) => {
    const savedDraft = await handleSave(updatedData); // Save first

    // Check if the save was successful before proceeding
    if (savedDraft && savedDraft.id) {
      console.log("Submitting data for draft ID:", savedDraft.id);
      try {
        // Use the ID from the fresh `savedDraft` object
        const response = await apiClient.post(`/content/${savedDraft.id}/generate`);
        console.log("Draft submitted:", response.data);
        router.push('/content');
      } catch (error) {
        console.error("Failed to submit draft:", error);
      }
    } else {
      console.error("Cannot submit because the draft failed to save or has no ID.");
    }
  };

  if (isLoading || !draftData) {
    return <div className="text-center p-12">Loading Editor...</div>;
  }

  // Look up the component from the map using the template name
  const EditorComponent = EDITOR_COMPONENT_MAP[draftData.templateId as keyof typeof EDITOR_COMPONENT_MAP];

  if (!EditorComponent) {
    return <div className="text-center p-12 text-red-500">Error: Unknown editor template "{draftData.templateId}".</div>;
  }

  return (
    <main className="p-8">
      {/* Render the dynamically selected component */}
      <EditorComponent 
        initialData={draftData} 
        onSave={handleSave}
        onSubmit={handleSubmit}
        isSaving={isSaving}
      />
    </main>
  );
}
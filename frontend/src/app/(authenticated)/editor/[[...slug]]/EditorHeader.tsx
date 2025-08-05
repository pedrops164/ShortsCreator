'use client';

import { Button } from "@/components/ui/button";
import { Loader2, Save, Send } from "lucide-react";
import { useRouter } from "next/navigation";
import { ArrowLeft } from "lucide-react";

interface EditorHeaderProps {
  editorTitle: string;
  onSave: () => void;
  onGenerate: () => void;
  isSaving: boolean;
  isSaveDisabled: boolean; // To disable save when no changes are made
}

export function EditorHeader({
  editorTitle,
  onSave,
  onGenerate,
  isSaving,
  isSaveDisabled
}: EditorHeaderProps) {
  const router = useRouter();

  return (
    <header className="sticky top-0 z-10 flex h-16 items-center gap-4 border-b bg-background/95 px-4 backdrop-blur-sm sm:px-6">
      <Button variant="outline" size="icon" className="h-8 w-8" onClick={() => router.back()}>
        <ArrowLeft className="h-4 w-4" />
        <span className="sr-only">Back</span>
      </Button>
      <h1 className="flex-1 text-xl font-semibold truncate">
        {editorTitle}
      </h1>
      <div className="flex items-center gap-2">
        <Button
          variant="outline"
          onClick={onSave}
          disabled={isSaveDisabled || isSaving}
        >
          {isSaving ? (
            <Loader2 className="mr-2 h-4 w-4 animate-spin" />
          ) : (
            <Save className="mr-2 h-4 w-4" />
          )}
          Save
        </Button>
        <Button
          onClick={onGenerate}
          disabled={isSaving}
        >
          <Send className="mr-2 h-4 w-4" />
          Generate...
        </Button>
      </div>
    </header>
  );
}
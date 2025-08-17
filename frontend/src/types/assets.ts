
// This defines the structure for a voice asset coming from the backend
export interface VoiceAsset {
  id: string; // The MongoDB document ID
  assetId: string; // e.g., "openai_alloy"
  name: string; // e.g., "Alloy"
  description: string;
  sourceUrl: string; // The public URL to the .mp3 file
}

// This defines the structure for a video asset coming from the backend
export interface VideoAsset {
    id: string;
    assetId: string;
    name: string;
    category: string;
    thumbnailUrl: string;
    sourceUrl: string;
}

export interface FontAsset {
    id: string;
    assetId: string;
    name: string;
    sourceUrl: string;
    displayName: string; // for better UI display
}
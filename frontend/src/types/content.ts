import { OutputAssetsV1 } from './outputAssets';     // Ensure correct import paths

export enum ContentStatus {
    DRAFT = 'DRAFT',
    PROCESSING = 'PROCESSING',
    COMPLETED = 'COMPLETED',
    FAILED = 'FAILED',
}

export type ContentType = 'REDDIT_STORY' | 'TWITTER_THREAD';

// The base interface reflecting the common fields of the backend Content model.
// This interface is generic to allow for different templateParams types.
export interface ContentBase<TTemplateParams> {
  id: string;
  userId: string;
  templateId: string;
  contentType: ContentType;
  status: ContentStatus;
  createdAt: string; // Instant is typically represented as ISO string (e.g., "2023-10-27T10:00:00Z")
  lastModifiedAt: string; // Instant is typically represented as ISO string

  progressPercentage?: number;

  // Using TTemplateParams allows us to specify the exact type
  // when extending this base type.
  templateParams: TTemplateParams;

  outputAssets?: OutputAssetsV1;
  errorMessage?: string;
}

// This interface matches the VideoStatusUpdate DTO on the backend
export interface VideoStatusUpdate {
    userId: string;
    contentId: string;
    status: ContentStatus;
    progressPercentage?: number; // Optional, used for PROCESSING status
}
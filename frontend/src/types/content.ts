import { ContentType, ContentStatus } from './enums'; // Ensure correct import paths
import { OutputAssetsV1 } from './outputAssets';     // Ensure correct import paths

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

  // Using TTemplateParams allows us to specify the exact type
  // when extending this base type.
  templateParams: TTemplateParams;

  outputAssets?: OutputAssetsV1;
  errorMessage?: string;
}
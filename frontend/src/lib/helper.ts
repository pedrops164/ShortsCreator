import { RedditStoryParams } from '@/types';
import { Draft } from '@/types/drafts';

/**
 * Gets the title from any draft type in a type-safe way.
 * @param draft The draft object.
 * @returns The title string, or a default value if not found.
 */
export function getDraftTitle(draft: Draft): string {
  switch (draft.contentType) {
    case 'REDDIT_STORY':
      // TypeScript knows `draft` is `RedditStoryDraft` here
      return (draft.templateParams as RedditStoryParams).postTitle ?? 'Untitled Reddit Story';

    default:
      return 'Untitled';
  }
}
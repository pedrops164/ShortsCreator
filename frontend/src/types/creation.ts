import { CharacterExplainsParams } from './templates/characterExplains';
import { RedditStoryParams } from './templates/redditStory';

/**
 * Represents the generic payload structure for creating a new content item.
 * This directly mirrors the backend's ContentCreationRequest DTO.
 * TTemplateParams allows for specific template parameter types.
 */
export interface ContentCreationPayload<TTemplateParams> {
  templateId: string;
  templateParams: TTemplateParams;
}

/**
 * Specific payload for creating a Reddit Story content item.
 * Uses literal types for templateId and contentType for better type safety
 * and to enable discriminated unions.
 */
// export type RedditStoryCreationPayload = ContentCreationPayload<RedditStoryParams>;
/*  & {
  templateId: 'reddit_story_v1';
  contentType: 'REDDIT_STORY';
}; */

// Specific payload for Reddit Story
export type RedditStoryCreationPayload = ContentCreationPayload<RedditStoryParams> & {
  templateId: 'reddit_story_v1';
};

// Specific payload for Character Explains
export type CharacterExplainsCreationPayload = ContentCreationPayload<CharacterExplainsParams> & {
    templateId: 'character_explains_v1';
};

/**
 * A union type representing all possible content creation payloads.
 * We add more types here as we introduce new content templates.
 */
export type CreationPayload = RedditStoryCreationPayload | CharacterExplainsCreationPayload;
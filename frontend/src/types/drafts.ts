import { ContentBase } from './content';
import { RedditStoryParams } from './templates/redditStory';
import { CharacterExplainsParams } from './templates/characterExplains';

// Specific draft type for Reddit Story, extending ContentBase
// and providing the concrete type for templateParams.
export type RedditStoryDraft = ContentBase<RedditStoryParams>
 & {
  templateId: 'reddit_story_v1'; // Ensures consistency for this specific type
  contentType: 'REDDIT_STORY';
};

export type CharacterExplainsDraft = ContentBase<CharacterExplainsParams>
 & {
  templateId: 'character_explains_v1'; // Ensures consistency for this specific type
  contentType: 'CHARACTER_EXPLAINS';
};

// The final union type
// We add more content types as needed
export type Draft = RedditStoryDraft | CharacterExplainsDraft;
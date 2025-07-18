export type RedditStoryParams = {
  backgroundVideoId?: 'minecraft1' | 'gta1'; // Not required by schema
  backgroundMusicId?: '' | 'fun_1' | 'mysterious_1'; // Optional based on schema
  avatarImageUrl?: string; // String with 'uri' format - TypeScript string is sufficient
  username?: string; // Not required by schema
  subreddit?: string; // Not required by schema
  postTitle?: string; // Not required by schema
  postDescription?: string; // Not required by schema
  comments?: Array<{
    author?: string; // Not required by schema
    text: string; // Required by schema
  }>;
  aspectRatio?: '9:16'; // Not required by schema
  showSubtitles?: boolean; // Not required by schema
  subtitlesColor?: string; // String with hex pattern - TypeScript string is sufficient
  subtitlesFont?: string; // Not required by schema
  subtitlesPosition?: 'bottom' | 'center' | 'top'; // Not required by schema
  voiceSelection?: 'openai_alloy' | 'openai_ash' | 'openai_ballad' | 'openai_coral' | 'openai_echo' | 'openai_fable' | 'openai_onyx' | 'openai_nova' | 'openai_sage' | 'openai_shimmer' | 'openai_verse'; // Not required by schema
  theme?: 'dark' | 'light'; // Not required by schema
};
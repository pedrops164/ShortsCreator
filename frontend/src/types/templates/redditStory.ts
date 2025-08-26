export type RedditStoryParams = {
  backgroundVideoId: string;
  backgroundMusicId: string;
  avatarImageUrl?: string; // String with 'uri' format - TypeScript string is sufficient
  username?: string; // Not required by schema
  subreddit?: string; // Not required by schema
  postTitle?: string; // Not required by schema
  postDescription?: string; // Not required by schema
  comments: Array<{
    author?: string; // Not required by schema
    text: string; // Required by schema
  }>;
  aspectRatio?: '9:16'; // Not required by schema
  subtitles: {
    show: boolean; // Optional, has a default
    color: string; // Optional, has a default
    font: string; // Optional, has a default
    position: 'bottom' | 'center' | 'top'; // Optional, has a default
  };
  voiceSelection: 'openai_alloy' | 'openai_ash' | 'openai_ballad' | 'openai_coral' | 'openai_echo' | 'openai_fable' | 'openai_onyx' | 'openai_nova' | 'openai_sage' | 'openai_shimmer' | 'openai_verse'; // Not required by schema
  theme: 'dark' | 'light'; // Not required by schema
};
export type CharacterExplainsParams = {
  characterPresetId?: 'peter_stewie' | 'rick_morty' | 'spongebob_patrick'; // Not required by schema (until submission)
  topicTitle?: string; // Not required by schema (until submission)
  dialogue?: Array<{
    characterId: string; // Required within a dialogue line object
    text: string; // Required within a dialogue line object
  }>;
  backgroundVideoId?: 'minecraft1' | 'gta1'; // Not required by schema (until submission)
  backgroundMusicId?: '' | 'fun_1' | 'mysterious_1'; // Optional
  aspectRatio?: '9:16'; // Not required by schema, has a default
  subtitles?: {
    show?: boolean; // Optional, has a default
    color?: string; // Optional, has a default
    font?: string; // Optional, has a default
    position?: 'bottom' | 'center'; // Optional, has a default
  };
};
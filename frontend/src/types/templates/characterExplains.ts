export type CharacterExplainsParams = {
  characterPresetId?: string; // This can stay optional as it has no default value
  topicTitle: string;         // Now required
  dialogue: Array<{          // Now required
    characterId: string;
    text: string;
  }>;
  backgroundVideoId: string; // Now required
  backgroundMusicId: string;   // Now required
  aspectRatio: '9:16';       // Now required
  subtitles: {               // Now required
    show: boolean;
    color: string;
    font: string;
    position: 'bottom' | 'center' | 'top';
  };
};
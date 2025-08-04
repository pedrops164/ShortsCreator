

export interface Character {
  characterId: string;
  name: string;
  avatarUrl: string; // Use the new, clear name
}

export interface CharacterPreset {
  id: string; // MongoDB ObjectId
  presetId: string; // Your human-readable ID
  name: string;
  characters: Character[];
}
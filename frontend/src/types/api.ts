

export interface GenerateTextRequest {
  generationType: 'REDDIT_POST_DESCRIPTION' | 'REDDIT_COMMENT' | 'CHARACTER_DIALOGUE';
  context: Record<string, string>;
}

export interface GeneratedContentResponse {
  generationType: string;
  content: {
    text?: string;
    comments?: string[];
    dialogue?: { character: string; line: string }[];
  };
}
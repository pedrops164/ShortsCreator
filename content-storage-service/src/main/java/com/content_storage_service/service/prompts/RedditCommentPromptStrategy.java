package com.content_storage_service.service.prompts;

import org.springframework.stereotype.Component;

import com.content_storage_service.enums.GenerationType;

import java.util.Map;

@Component
public class RedditCommentPromptStrategy implements PromptStrategy {

    @Override
    public GenerationType getGenerationType() {
        return GenerationType.REDDIT_COMMENT;
    }

    @Override
    public String createPrompt(Map<String, String> context) {
        String postTitle = context.get("postTitle");
        if (postTitle == null || postTitle.isBlank()) {
            throw new IllegalArgumentException("Context must contain 'postTitle' for Reddit post generation.");
        }
        return String.format(
            """
            A Reddit post has the title: "%s".

            Your task is to generate 3 to 5 diverse, authentic-sounding Reddit comments for this post. The comments should reflect different user perspectives (e.g., one serious, one witty, etc).

            **IMPORTANT**: Your response **MUST** be a valid JSON array of strings. Do not include any other text, explanations, or markdown formatting. Your entire output should be parsable as a JSON array.

            Example format:
            [
              "This is the first comment, offering a serious take on the matter.",
              "I completely disagree, and here's a funny reason why...",
              "Matter of fact, something crazy happened to me once related to this."
            ]
            """,
            postTitle
        );
    }
}
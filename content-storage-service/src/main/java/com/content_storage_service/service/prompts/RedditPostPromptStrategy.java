package com.content_storage_service.service.prompts;

import org.springframework.stereotype.Component;

import com.content_storage_service.enums.GenerationType;

import java.util.Map;

@Component
public class RedditPostPromptStrategy implements PromptStrategy {

    @Override
    public GenerationType getGenerationType() {
        return GenerationType.REDDIT_POST_DESCRIPTION;
    }

    @Override
    public String createPrompt(Map<String, String> context) {
        String postTitle = context.get("postTitle");
        if (postTitle == null || postTitle.isBlank()) {
            throw new IllegalArgumentException("Context must contain 'postTitle' for Reddit post generation.");
        }
        // This is where you engineer your high-quality prompt
        return String.format(
            "You are an expert Reddit storyteller. Write a compelling and interesting, first-person Reddit post body for the title: \"%s\". " +
            "The story should be engaging, detailed, and written in a style suitable for a subreddit like r/AskReddit or r/AmItheAsshole or r/tifu. " +
            "Ensure the post is long enough to be narrated in a short video (at least 300 words). Do not include the title in your response.",
            postTitle
        );
    }
}
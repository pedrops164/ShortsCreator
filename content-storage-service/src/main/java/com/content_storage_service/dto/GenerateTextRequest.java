package com.content_storage_service.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

import com.content_storage_service.enums.GenerationType;

public record GenerateTextRequest(
    @NotNull
    GenerationType generationType,

    // Contextual data needed for the prompt.
    // e.g., for REDDIT_COMMENT, keys could be "postTitle", "postDescription".
    // e.g., for CHARACTER_DIALOGUE, key could be "topic".
    @NotNull
    Map<String, String> context
) {
}
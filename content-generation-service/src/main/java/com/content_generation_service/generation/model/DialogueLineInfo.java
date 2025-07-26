package com.content_generation_service.generation.model;

import lombok.Data;

/**
 * A record to hold metadata for a single line in the character dialogue.
 * This is used to sync character images with the narration.
 */
@Data
public class DialogueLineInfo {
    private final String characterId;
    private final double startTime;
    private final double duration;
}
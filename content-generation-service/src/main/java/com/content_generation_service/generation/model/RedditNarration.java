package com.content_generation_service.generation.model;

import java.nio.file.Path;
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Specialized version of NarrationSegment for Reddit, with title duration information.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RedditNarration extends NarrationSegment {
    private double titleDurationSeconds;

    // FIX: Create a manual constructor instead of using @AllArgsConstructor
    public RedditNarration(Path audioFilePath, double durationSeconds, List<WordTiming> wordTimings, double titleDurationSeconds) {
        // Explicitly call the parent's all-argument constructor
        super(audioFilePath, durationSeconds, wordTimings);
        // Set the field for this specific class
        this.titleDurationSeconds = titleDurationSeconds;
    }
}

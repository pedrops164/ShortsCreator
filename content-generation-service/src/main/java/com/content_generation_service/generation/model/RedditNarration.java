package com.content_generation_service.generation.model;

import java.nio.file.Path;
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RedditNarration extends NarrationSegment {
    private double titleDurationSeconds;

    // Create a manual constructor instead of using @AllArgsConstructor
    public RedditNarration(Path audioFilePath, double durationSeconds, List<WordTiming> wordTimings, double titleDurationSeconds) {
        super(audioFilePath, durationSeconds, wordTimings);
        this.titleDurationSeconds = titleDurationSeconds;
    }
}

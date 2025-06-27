package com.content_generation_service.generation.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.nio.file.Path;
import java.util.List;

/**
 * Represents a single, generated piece of narration audio.
 */
@Data
@AllArgsConstructor
public class NarrationSegment {
    private Path audioFilePath;
    private double durationSeconds;
    private List<WordTiming> wordTimings;

    public double getLastWordEndTime() {
        if (wordTimings == null || wordTimings.isEmpty()) {
            // If no timings, fall back to the calculated audio duration
            return this.durationSeconds; 
        }
        // Return the end time of the very last word in the list
        return wordTimings.get(wordTimings.size() - 1).getEndTimeSeconds();
    }
}
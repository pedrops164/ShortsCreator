package com.content_generation_service.generation.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single word and its start and end time in an audio track.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WordTiming extends TimeRange {
    private String word;

    // Custom constructor with the desired parameter order
    public WordTiming(String word, double startTimeSeconds, double endTimeSeconds) {
        super(startTimeSeconds, endTimeSeconds); // Call the parent class constructor
        this.word = word;
    }
}

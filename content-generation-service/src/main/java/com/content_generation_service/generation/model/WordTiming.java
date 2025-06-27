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
public class WordTiming {
    private String word;
    private double startTimeSeconds;
    private double endTimeSeconds;
}

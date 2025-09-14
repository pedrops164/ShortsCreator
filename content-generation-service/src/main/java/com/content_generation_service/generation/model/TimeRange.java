package com.content_generation_service.generation.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a timing interval with a start and end time.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeRange {
    protected double startTimeSeconds;
    protected double endTimeSeconds;
}

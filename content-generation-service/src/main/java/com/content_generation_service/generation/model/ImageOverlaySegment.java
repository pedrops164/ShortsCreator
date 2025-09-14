package com.content_generation_service.generation.model;

import java.nio.file.Path;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ImageOverlaySegment {
    private Path imagePath;
    private double durationSeconds;
    private TimeRange timeRange;
    private ImagePosition position;
}

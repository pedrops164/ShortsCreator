package com.content_generation_service.generation.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

// This DTO will hold the parsed data from ffprobe's JSON output
public record VideoMetadata(double duration, int width, int height) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FfprobeOutput(List<Stream> streams) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Stream(
        int width,
        int height,
        @JsonProperty("duration") String durationString
    ) {}
}
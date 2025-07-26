package com.content_generation_service.generation.service.speechify.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

// A simple DTO (Data Transfer Object) to map the JSON response
@Data
public class SpeechifyAudioResponse {

    @JsonProperty("audio_data")
    private String audioData; // This will contain the Base64 encoded audio

    @JsonProperty("audio_format")
    private String audioFormat;

    @JsonProperty("billable_characters_count")
    private long billableCharactersCount;

    @JsonProperty("speech_marks")
    private SpeechMarks speechMarks;

    @Data
    public static class SpeechMarks {
        private List<SpeechMark> chunks; // List of word timings
        private Long end;
        private Double end_time;
        private Long start;
        private Double start_time;
        private String type; // e.g., "word", "sentence", etc.
        private String value; // The actual word or sentence text
    }

    @Data
    public static class SpeechMark {
        private Long end;
        private Long end_time;
        private Long start;
        private Long start_time;
        private String type; // e.g., "word", "sentence", etc.
        private String value; // The actual word or sentence text
    }
}
package com.content_generation_service.generation.service.speechify.dto;

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
}
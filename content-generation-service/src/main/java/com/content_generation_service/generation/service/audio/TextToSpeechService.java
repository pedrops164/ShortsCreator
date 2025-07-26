package com.content_generation_service.generation.service.audio;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

/**
 * An abstract base service for handling text-to-speech generation.
 * It manages TTS providers and provides common utilities for audio manipulation,
 * such as combining audio tracks with FFmpeg and adjusting word timestamps.
 */
@Slf4j
@Service
public class TextToSpeechService {

    protected final Map<String, TextToSpeechProvider> providerMap;

    /**
     * Constructs the service by creating a map of available TTS providers.
     * @param providers A list of all beans implementing TextToSpeechProvider, injected by Spring.
     */
    protected TextToSpeechService(List<TextToSpeechProvider> providers) {
        this.providerMap = providers.stream()
                .collect(Collectors.toMap(TextToSpeechProvider::getProviderId, Function.identity()));
        log.info("Initialized TextToSpeechService with providers: {}", providerMap.keySet());
    }

    public TextToSpeechProvider getProvider(String providerId) {
        TextToSpeechProvider provider = providerMap.get(providerId);
        if (provider == null) {
            throw new IllegalArgumentException("No TTS provider found for ID: " + providerId);
        }
        return provider;
    }

    /**
     * A small, immutable data carrier for the parsed voice ID.
     */
    public record ParsedVoiceId(String providerId, String voiceId) {}

    /**
     * Parses a global voice ID string (e.g., "openai_echo") into its provider and voice components.
     * @param globalVoiceId The combined ID string.
     * @return A ParsedVoiceId containing the provider and voice IDs.
     * @throws IllegalArgumentException if the format is invalid.
     */
    public static ParsedVoiceId parseGlobalVoiceId(String globalVoiceId) {
        if (globalVoiceId == null || !globalVoiceId.contains("_")) {
            throw new IllegalArgumentException("Invalid globalVoiceId format. Expected 'provider_voiceid', but got: " + globalVoiceId);
        }
        String[] parts = globalVoiceId.split("_", 2);
        if (parts.length < 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new IllegalArgumentException("Invalid globalVoiceId format. Both provider and voiceid must be non-empty.");
        }
        return new ParsedVoiceId(parts[0], parts[1]);
    }
}
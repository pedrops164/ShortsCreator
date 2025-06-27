package com.content_generation_service.generation.service.audio;

import com.content_generation_service.generation.model.WordTiming;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.List;

/**
 * High-level service for handling audio transcription. It delegates the actual
 * work to a configured TranscriptionProvider implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranscriptionService {

    // Spring will inject the correct implementation (e.g., OpenAiTranscriptionProvider)
    // based on the active configuration profile and properties.
    private final TranscriptionProvider transcriptionProvider;

    /**
     * Takes an audio file and retrieves its word-level timing information.
     *
     * @param audioFilePath The path to the local audio file.
     * @return A Mono emitting a list of WordTiming objects.
     */
    public Mono<List<WordTiming>> getWordTimings(Path audioFilePath) {
        log.info("Delegating transcription to provider: {}", transcriptionProvider.getProviderId());
        return transcriptionProvider.getWordTimings(audioFilePath);
    }
}

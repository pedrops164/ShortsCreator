package com.content_generation_service.generation.service.audio;

import com.content_generation_service.generation.model.WordTiming;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.List;

/**
 * An interface representing a provider for transcription services
 * capable of generating word-level timestamps (forced alignment).
 */
public interface TranscriptionProvider {

    /**
     * Transcribes an audio file to extract word-level timing information.
     *
     * @param audioFilePath The path to the local audio file to be transcribed.
     * @return A Mono emitting a list of WordTiming objects.
     */
    Mono<List<WordTiming>> getWordTimings(Path audioFilePath);

    /**
     * Indicates which provider this implementation represents (e.g., "openai").
     * @return The unique key for this provider.
     */
    String getProviderId();
}
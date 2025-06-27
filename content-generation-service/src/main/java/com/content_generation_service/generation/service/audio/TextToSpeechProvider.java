package com.content_generation_service.generation.service.audio;

import com.content_generation_service.generation.model.NarrationSegment;
import reactor.core.publisher.Mono;

/**
 * An interface representing a provider for Text-to-Speech services.
 * This abstraction allows for easily swapping different TTS APIs (OpenAI, ElevenLabs, etc.).
 */
public interface TextToSpeechProvider {

    /**
     * Generates a single segment of audio from the given text using a specific voice.
     *
     * @param text The text to narrate.
     * @param voiceId The identifier for the voice to use (specific to the provider).
     * @param generateTimings If true, perform the extra step of extracting word-level timings.
     * @return A Mono emitting the NarrationSegment, containing the audio file path and its metadata.
     */
    Mono<NarrationSegment> generate(String text, String voiceId, boolean generateTimings);

    /**
     * Indicates which provider this implementation represents (e.g., "openai").
     * @return The unique key for this provider.
     */
    String getProviderId();
}
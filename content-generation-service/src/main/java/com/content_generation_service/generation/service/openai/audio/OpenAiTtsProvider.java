package com.content_generation_service.generation.service.openai.audio;

import com.content_generation_service.config.AppProperties;
import com.content_generation_service.generation.model.NarrationSegment;
import com.content_generation_service.generation.model.WordTiming;
import com.content_generation_service.generation.service.audio.TextToSpeechProvider;
import com.content_generation_service.generation.service.audio.TranscriptionProvider;

import lombok.extern.slf4j.Slf4j;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Concrete implementation of a TextToSpeechProvider using OpenAI's TTS API.
 * This bean is only created if the property 'app.tts.provider' is set to 'openai'.
 */
@Slf4j
@Service
public class OpenAiTtsProvider implements TextToSpeechProvider {

    private final WebClient webClient;
    private final String apiKey;
    private final TranscriptionProvider transcriptionService;

    public OpenAiTtsProvider(WebClient.Builder webClientBuilder, AppProperties appProperties, TranscriptionProvider transcriptionService) {
        this.webClient = webClientBuilder.baseUrl("https://api.openai.com/v1/audio").build();
        this.apiKey = appProperties.getTts().getOpenai().getApiKey();
        this.transcriptionService = transcriptionService;
        if (apiKey == null || apiKey.isBlank()) {
            log.error("OpenAI API key is missing. Please set the OPENAI_API_KEY environment variable.");
            throw new IllegalArgumentException("OpenAI API key is not configured.");
        }
    }

    @Override
    public String getProviderId() {
        return "openai";
    }

    @Override
    public Mono<NarrationSegment> generate(String text, String voiceId, boolean generateTimings) {
        log.info("Requesting narration from OpenAI. Voice: [{}], Generate Timings: {}", voiceId, generateTimings);

        Map<String, Object> requestBody = Map.of("model", "tts-1-hd", "input", text, "voice", voiceId);

        try {
            // Create a temporary file path first
            Path tempAudioFile = Files.createTempFile("openai-narration-" + UUID.randomUUID(), ".mp3");

            // Stream the response directly to the file
            Flux<DataBuffer> audioStream = webClient.post()
                    .uri("/speech")
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(DataBuffer.class)
                    .doOnError(e -> log.error("Failed during OpenAI TTS API call", e));
            
            // DataBufferUtils.write returns a Mono<Void> that completes when the file is fully written.
            return DataBufferUtils.write(audioStream, tempAudioFile, StandardOpenOption.CREATE)
                .then(Mono.defer(() -> {
                    // Process the file only AFTER it has been fully written
                    log.debug("Successfully streamed OpenAI audio to temporary file: {}", tempAudioFile);
                    return processAudioFile(tempAudioFile, generateTimings);
                }))
                .doOnError(err -> {
                    // Cleanup in case of an error during streaming or processing
                    try { Files.deleteIfExists(tempAudioFile); } catch (IOException e) { log.warn("Failed to cleanup temp file on error: {}", tempAudioFile); }
                });

        } catch (IOException e) {
            log.error("Failed to create temporary file for OpenAI audio", e);
            return Mono.error(e);
        }
    }

    /**
     * Processes a fully downloaded audio file to extract duration and word timings.
     */
    private Mono<NarrationSegment> processAudioFile(Path audioFile, boolean generateTimings) {
        double realDuration = getAudioDuration(audioFile);

        Mono<List<WordTiming>> timingsMono;
        if (generateTimings) {
            timingsMono = transcriptionService.getWordTimings(audioFile)
                .map(timings -> {
                    // Your existing logic to trim the last word timing
                    if (!timings.isEmpty()) {
                        WordTiming lastTiming = timings.get(timings.size() - 1);
                        if (lastTiming.getEndTimeSeconds() > realDuration) {
                            lastTiming.setEndTimeSeconds(realDuration);
                        }
                    }
                    return timings;
                });
        } else {
            timingsMono = Mono.just(Collections.emptyList());
        }

        return timingsMono.map(timings -> new NarrationSegment(audioFile, realDuration, timings));
    }

    /**
     * Calculates the duration of an audio file using the JAudiotagger library.
     * @param audioFile Path to the audio file.
     * @return The duration in seconds.
     */
    private double getAudioDuration(Path audioFile) {
        try {
            AudioFile f = AudioFileIO.read(audioFile.toFile());
            double duration = f.getAudioHeader().getPreciseTrackLength();
            log.debug("Calculated audio duration: {} seconds for file {}", duration, audioFile);
            return duration;
        } catch (Exception e) {
            log.warn("Could not calculate audio duration for file: {}. Falling back to 0. Reason: {}", audioFile, e.getMessage());
            return 0.0;
        }
    }
}

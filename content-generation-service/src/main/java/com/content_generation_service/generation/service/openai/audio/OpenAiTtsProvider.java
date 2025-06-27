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
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
@ConditionalOnProperty(name = "app.tts.provider", havingValue = "openai")
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

        return webClient.post()
            .uri("/speech")
            .header("Authorization", "Bearer " + apiKey)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(byte[].class)
            .flatMap(audioBytes -> saveAndProcessAudio(audioBytes, generateTimings))
            .doOnError(e -> log.error("Failed during OpenAI TTS API call", e));
    }

    private Mono<NarrationSegment> saveAndProcessAudio(byte[] audioBytes, boolean generateTimings) {
        Path tempAudioFile = null;
        try {
            tempAudioFile = Files.createTempFile("openai-narration-" + UUID.randomUUID(), ".mp3");
            Files.write(tempAudioFile, audioBytes);
            log.debug("Successfully saved OpenAI audio to temporary file: {}", tempAudioFile);

            // Calculate the real duration of the MP3 file
            double realDuration = getAudioDuration(tempAudioFile);

            // Conditionally generate word timings
            Mono<List<WordTiming>> timingsMono;
            if (generateTimings) {
                timingsMono = transcriptionService.getWordTimings(tempAudioFile);
                // trim last word timing to match the real duration
                timingsMono = timingsMono.map(timings -> {
                    if (!timings.isEmpty()) {
                        WordTiming lastTiming = timings.get(timings.size() - 1);
                        if (lastTiming.getEndTimeSeconds() > realDuration) {
                            lastTiming.setEndTimeSeconds(realDuration);
                            log.debug("Adjusted last word timing end time to match real duration: {}", realDuration);
                        }
                    }
                    return timings;
                });
            } else {
                log.debug("Skipping word timing generation as requested.");
                timingsMono = Mono.just(Collections.emptyList());
            }

            // After timings are generated (or skipped), create the final segment.
            // Note: We don't delete the temp file here; that's the orchestrator's job after video composition.
            Path finalTempAudioFile = tempAudioFile; // Effectively final for lambda
            return timingsMono.map(timings -> new NarrationSegment(finalTempAudioFile, realDuration, timings));

        } catch (IOException e) {
            log.error("Failed to write OpenAI audio to temporary file", e);
            // Clean up the file if it was created before the error
            if (tempAudioFile != null) {
                try {
                    Files.delete(tempAudioFile);
                } catch (IOException cleanupEx) {
                    log.error("Failed to clean up temporary audio file: {}", tempAudioFile, cleanupEx);
                }
            }
            return Mono.error(e);
        }
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

package com.content_generation_service.generation.service.openai.audio;

import com.content_generation_service.config.AppProperties;
import com.content_generation_service.generation.model.NarrationSegment;
import com.content_generation_service.generation.service.audio.TextToSpeechProvider;
import com.content_generation_service.generation.service.audio.TranscriptionProvider;
import com.content_generation_service.util.ResourceHelperService;

import lombok.extern.slf4j.Slf4j;
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
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class OpenAiTtsProvider implements TextToSpeechProvider {

    private final WebClient webClient;
    private final String apiKey;
    private final TranscriptionProvider transcriptionProvider;
    private final ResourceHelperService resourceHelperService;

    public OpenAiTtsProvider(WebClient.Builder webClientBuilder, AppProperties appProperties, TranscriptionProvider transcriptionProvider, ResourceHelperService resourceHelperService) {
        this.webClient = webClientBuilder.baseUrl("https://api.openai.com/v1/audio").build();
        this.apiKey = appProperties.getTts().getOpenai().getApiKey();
        this.transcriptionProvider = transcriptionProvider;
        this.resourceHelperService = resourceHelperService;
        if (apiKey == null || apiKey.isBlank()) {
            log.error("OpenAI API key is missing. Please set the OPENAI_API_KEY environment variable.");
            throw new IllegalArgumentException("OpenAI API key is not configured.");
        }
    }

    @Override
    public String getProviderId() {
        return "openai";
    }

    /**
     * Orchestrates the TTS generation process.
     * It first creates the audio file, then generates the narration segment from it.
     */
    @Override
    public Mono<NarrationSegment> generate(String text, String voiceId, boolean generateTimings) {
        log.info("Requesting narration from OpenAI. Voice: [{}], Generate Timings: {}", voiceId, generateTimings);
        
        // Use a variable to hold the path for cleanup purposes
        final Path[] tempAudioFile = new Path[1]; 

        return createAudioFile(text, voiceId)
                .doOnNext(path -> tempAudioFile[0] = path) // Store the path when it's created
                .flatMap(path -> transcriptionProvider.getNarrationSegmentFromAudioFile(path, generateTimings))
                .doOnError(err -> {
                    // Centralized cleanup: If any step fails, delete the temp file.
                    if (tempAudioFile[0] != null) {
                        resourceHelperService.deleteTemporaryFile(tempAudioFile[0]);
                    }
                });
    }

    /**
     * Calls the OpenAI TTS API to generate an audio file from text.
     * @return A Mono emitting the Path to the temporary audio file.
     */
    private Mono<Path> createAudioFile(String text, String voiceId) {
        Map<String, Object> requestBody = Map.of("model", "tts-1-hd", "input", text, "voice", voiceId);

        try {
            Path tempFile = Files.createTempFile("openai-narration-" + UUID.randomUUID(), ".mp3");
            
            Flux<DataBuffer> audioStream = webClient.post()
                    .uri("/speech")
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(DataBuffer.class);
            
            return DataBufferUtils.write(audioStream, tempFile, StandardOpenOption.CREATE)
                    .then(Mono.fromCallable(() -> {
                        log.debug("Successfully streamed OpenAI audio to temporary file: {}", tempFile);
                        return tempFile;
                    }));

        } catch (IOException e) {
            log.error("Failed to create temporary file for OpenAI audio", e);
            return Mono.error(e);
        }
    }
}
package com.content_generation_service.generation.service.speechify.audio;

import com.content_generation_service.config.AppProperties;
import com.content_generation_service.generation.model.NarrationSegment;
import com.content_generation_service.generation.service.audio.TextToSpeechProvider;
import com.content_generation_service.generation.service.audio.TranscriptionProvider;
import com.content_generation_service.generation.service.speechify.dto.SpeechifyAudioResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
public class SpeechifyVoiceCloningProvider implements TextToSpeechProvider {

    private final WebClient webClient;
    private final String apiKey;
    private final TranscriptionProvider transcriptionProvider;
    private final Map<String, String> voiceMapping;

    public SpeechifyVoiceCloningProvider(
            WebClient.Builder webClientBuilder,
            AppProperties appProperties,
            TranscriptionProvider transcriptionProvider) {

        this.webClient = webClientBuilder.baseUrl("https://api.sws.speechify.com/v1").build();
        this.apiKey = appProperties.getTts().getSpeechify().getApiKey();
        this.transcriptionProvider = transcriptionProvider;
        this.voiceMapping = appProperties.getTts().getSpeechify().getVoiceMapping();

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("Speechify API key is not configured.");
        }
    }

    @Override
    public String getProviderId() {
        return "speechify-clone";
    }

    @Override
    public Mono<NarrationSegment> generate(String text, String voiceId, boolean generateTimings) {
        log.info("Initiating voice clone generation for character '{}' with Speechify.", voiceId);

        // Look up the provider-specific voice ID from the configuration map.
        String speechifyVoiceId = this.voiceMapping.get(voiceId);
        if (speechifyVoiceId == null) {
            return Mono.error(new IllegalArgumentException("No Speechify voice ID mapping found for character: " + voiceId));
        }

        // The logic becomes a simple chain.
        return createAudioFile(text, speechifyVoiceId)
                .flatMap(outputFile -> transcriptionProvider.getNarrationSegmentFromAudioFile(outputFile, generateTimings));
    }

    /**
     * Calls the Speechify API to generate audio using a pre-registered voice ID.
     * @param text The text to synthesize.
     * @param speechifyVoiceId The actual voice ID from Speechify.
     * @return A Mono emitting the Path to the temporary output audio file.
     */
    private Mono<Path> createAudioFile(String text, String speechifyVoiceId) {

        Map<String, Object> requestBody = Map.of(
            "input", text,
            "voice_id", speechifyVoiceId,
            "audio_format", "mp3",
            "model", "simba-english"
        );

        return webClient.post()
            .uri("/audio/speech")
            .header("Authorization", "Bearer " + this.apiKey)
            .bodyValue(requestBody)
            .retrieve()
            .onStatus(httpStatus -> httpStatus.isError(), clientResponse ->
                clientResponse.bodyToMono(String.class)
                    .flatMap(errorBody -> {
                        log.error("Speechify API Error - Status: {}, Body: {}", clientResponse.statusCode(), errorBody);
                        return Mono.error(new IOException("Speechify API call failed with status " + clientResponse.statusCode()));
                    })
            )
            // Receive the response as the SpeechifyAudioResponse DTO.
            .bodyToMono(SpeechifyAudioResponse.class)
            .flatMap(response -> {
                try {
                    // Decode the Base64 string into raw audio bytes.
                    byte[] audioBytes = Base64.getDecoder().decode(response.getAudioData());

                    // Create a temporary file and write the decoded bytes to it.
                    Path tempOutputFile = Files.createTempFile("speechify-tts-", ".mp3");
                    Files.write(tempOutputFile, audioBytes);
                    
                    log.debug("Successfully decoded and wrote Speechify audio to temporary file: {}", tempOutputFile);
                    return Mono.just(tempOutputFile);
                } catch (IOException e) {
                    log.error("Failed to write decoded audio to file", e);
                    return Mono.error(e);
                }
            });
    }
}
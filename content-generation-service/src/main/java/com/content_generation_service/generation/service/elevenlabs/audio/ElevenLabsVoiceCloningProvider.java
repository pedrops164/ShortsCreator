package com.content_generation_service.generation.service.elevenlabs.audio;

import com.content_generation_service.config.AppProperties;
import com.content_generation_service.generation.model.NarrationSegment;
import com.content_generation_service.generation.service.audio.TextToSpeechProvider;
import com.content_generation_service.generation.service.audio.TranscriptionProvider;
import com.content_generation_service.util.ResourceHelperService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Slf4j
@Service
public class ElevenLabsVoiceCloningProvider implements TextToSpeechProvider {

    private final WebClient webClient;
    private final String apiKey;
    private final TranscriptionProvider transcriptionProvider;
    private final String characterAudioBasePath;
    private final ResourceHelperService resourceHelperService;

    public ElevenLabsVoiceCloningProvider(
            WebClient.Builder webClientBuilder,
            AppProperties appProperties,
            TranscriptionProvider transcriptionProvider,
            ResourceHelperService resourceHelperService) {

        this.webClient = webClientBuilder.baseUrl("https://api.elevenlabs.io/v1").build();
        this.apiKey = appProperties.getTts().getElevenlabs().getApiKey();
        this.characterAudioBasePath = appProperties.getTts().getCloning().getAudioDirPath();
        this.transcriptionProvider = transcriptionProvider;
        this.resourceHelperService = resourceHelperService;

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("ElevenLabs API key is not configured.");
        }
        if (characterAudioBasePath == null || characterAudioBasePath.isBlank()) {
            throw new IllegalArgumentException("Path for character voice cloning audio is not configured.");
        }
    }

    @Override
    public String getProviderId() {
        return "elevenlabs-clone";
    }

    @Override
    public Mono<NarrationSegment> generate(String text, String voiceId, boolean generateTimings) {
        log.info("Initiating voice clone generation for character '{}' with ElevenLabs.", voiceId);

        // This path is now a classpath resource path, not a filesystem path
        String resourcePath = characterAudioBasePath + "/" + voiceId + ".mp3";
        Path tempSampleFile = null;
        try {
            // Create a temporary file from the classpath resource
            tempSampleFile = resourceHelperService.createTempFileFromClasspath(resourcePath);
            final Path finalTempSampleFile = tempSampleFile; // Final variable for lambda

            // Use the temporary file to make the API call
            return createAudioFile(text, finalTempSampleFile)
                    // The rest of the chain is the same
                    .flatMap(outputFile -> transcriptionProvider.getNarrationSegmentFromAudioFile(outputFile, generateTimings))
                    .doFinally(signalType -> resourceHelperService.deleteTemporaryFile(finalTempSampleFile)); // Clean up the temp sample file

        } catch (IOException e) {
            return Mono.error(new RuntimeException("Failed to load character voice from classpath: " + resourcePath, e));
        }
    }

    /**
     * Calls the ElevenLabs API to generate an audio file using a voice clone sample.
     * This uses a multipart request to upload the voice file.
     * @return A Mono emitting the Path to the temporary output audio file.
     */
    private Mono<Path> createAudioFile(String text, Path audioSamplePath) {
        Path tempOutputFile;
        try {
            tempOutputFile = Files.createTempFile("elevenlabs-clone-" + UUID.randomUUID(), ".mp3");
        } catch (IOException e) {
            log.error("Failed to create temporary output file for ElevenLabs audio", e);
            return Mono.error(e);
        }

        // Build the multipart request body
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("text", text);
        bodyBuilder.part("files", new FileSystemResource(audioSamplePath));
        // You can add more parts here for voice settings if needed, e.g., stability

        Flux<DataBuffer> audioStream = webClient.post()
                .uri("/voices/add") // Using the endpoint to add a voice from a sample
                .header("xi-api-key", this.apiKey)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .retrieve()
                .bodyToFlux(DataBuffer.class);

        return DataBufferUtils.write(audioStream, tempOutputFile, StandardOpenOption.CREATE)
                .then(Mono.fromCallable(() -> {
                    log.debug("Successfully streamed ElevenLabs audio to temporary file: {}", tempOutputFile);
                    return tempOutputFile;
                }));
    }
}
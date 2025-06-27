package com.content_generation_service.generation.service.openai.audio;

import com.content_generation_service.config.AppProperties;
import com.content_generation_service.generation.model.WordTiming;
import com.content_generation_service.generation.service.audio.TranscriptionProvider;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete implementation of a TranscriptionProvider using OpenAI's Whisper API.
 * This bean is only created if the property 'app.transcription.provider' is set to 'openai'.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.transcription.provider", havingValue = "openai")
public class OpenAiTranscriptionProvider implements TranscriptionProvider {

    private final WebClient webClient;
    private final String apiKey;

    public OpenAiTranscriptionProvider(WebClient.Builder webClientBuilder, AppProperties appProperties) {
        // The transcription API can take a while, so we increase the timeout.
        HttpClient httpClient = HttpClient.create().responseTimeout(Duration.ofMinutes(2));
        this.webClient = webClientBuilder
                .baseUrl("https://api.openai.com/v1/audio")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
                
        this.apiKey = appProperties.getTts().getOpenai().getApiKey(); // Re-using the same API key
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("OpenAI API key is not configured for transcription.");
        }
    }

    @Override
    public String getProviderId() {
        return "openai";
    }

    @Override
    public Mono<List<WordTiming>> getWordTimings(Path audioFilePath) {
        log.info("Requesting word-level transcription from OpenAI for audio file: {}", audioFilePath);

        // Build the multipart/form-data request body
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(audioFilePath));
        body.add("model", "whisper-1");
        body.add("response_format", "verbose_json");
        body.add("timestamp_granularities[]", "segment"); // or word

        return webClient.post()
                .uri("/transcriptions")
                .header("Authorization", "Bearer " + apiKey)
                .body(BodyInserters.fromMultipartData(body))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(this::parseTimingsFromResponse)
                .doOnError(e -> log.error("Failed during OpenAI transcription API call for file {}", audioFilePath, e));
    }

    /**
     * Parses the 'verbose_json' response from the Whisper API to extract word timings.
     * @param response The top-level JsonNode of the API response.
     * @return A list of WordTiming objects.
     */
    private List<WordTiming> parseTimingsFromResponse(JsonNode response) {
        List<WordTiming> timings = new ArrayList<>();
        if (response.has("segments")) {
            for (JsonNode wordNode : response.get("segments")) {
                String word = wordNode.get("text").asText();
                double start = wordNode.get("start").asDouble();
                double end = wordNode.get("end").asDouble();
                timings.add(new WordTiming(word, start, end));
            }
            log.debug("Successfully parsed {} word timings from OpenAI response.", timings.size());
        } else {
            log.warn("OpenAI transcription response did not contain a 'segments' array. Full text: {}", response.get("text"));
        }
        return timings;
    }
}

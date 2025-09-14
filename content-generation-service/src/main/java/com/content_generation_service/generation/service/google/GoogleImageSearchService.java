package com.content_generation_service.generation.service.google;

import com.content_generation_service.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class GoogleImageSearchService {

    private final WebClient.Builder webClientBuilder;
    private final AppProperties appProperties;
    
    @Value("${app.storage.shared-temp.base-path}")
    private String sharedTempBasePath;

    private static final String GOOGLE_SEARCH_API_URL = "https://www.googleapis.com/customsearch/v1";

    public Mono<Path> downloadImageForQuery(String query) {
        log.info("Searching for image with query: '{}'", query);
        // return hardcoded image for testing
        //return Mono.just(Path.of("/home/pedro/app-dev/shared-temp-storage/a337a40d-3d5b-49c6-852c-69e67ab9dce4.jpg"));
        return findFirstImageUrl(query)
            .flatMap(this::downloadImageToTempFile)
            .doOnError(e -> log.error("Failed to download image for query '{}'", query, e));
    }

    private Mono<String> findFirstImageUrl(String query) {
        URI searchUri = UriComponentsBuilder.fromUriString(GOOGLE_SEARCH_API_URL)
            .queryParam("key", appProperties.getGoogle().getApiKey())
            .queryParam("cx", appProperties.getGoogle().getCseId())
            .queryParam("q", query)
            .queryParam("searchType", "image")
            .queryParam("imgSize", "large") // Request large images
            .queryParam("num", 1) // We only need the top result
            .build()
            .toUri();

        return webClientBuilder.build()
            .get()
            .uri(searchUri)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(response -> response.path("items").get(0).path("link").asText())
            .filter(link -> !link.isEmpty())
            .switchIfEmpty(Mono.error(new RuntimeException("No image found for query: " + query)));
    }

    private Mono<Path> downloadImageToTempFile(String imageUrl) {
        String browserUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36";

        return webClientBuilder.build()
            .get()
            .uri(imageUrl)
            .header("User-Agent", browserUserAgent) // mimic a real browser
            .retrieve()
            .bodyToMono(byte[].class)
            .flatMap(imageBytes -> {
                try {
                    Path tempDir = Path.of(sharedTempBasePath);
                    Files.createDirectories(tempDir);
                    // Use a unique name to avoid collisions
                    String fileName = UUID.randomUUID().toString() + ".jpg";
                    Path tempFile = Files.createFile(tempDir.resolve(fileName));
                    Files.write(tempFile, imageBytes);
                    log.info("Successfully downloaded image to {}", tempFile);
                    return Mono.just(tempFile);
                } catch (IOException e) {
                    return Mono.error(new RuntimeException("Failed to save downloaded image to temp file", e));
                }
            });
    }
}
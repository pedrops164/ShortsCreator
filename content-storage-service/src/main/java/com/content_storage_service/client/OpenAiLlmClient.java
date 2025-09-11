package com.content_storage_service.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.content_storage_service.config.AppProperties;
import com.content_storage_service.dto.OpenAiChatMessage;
import com.content_storage_service.dto.OpenAiChatRequest;
import com.content_storage_service.dto.OpenAiChatResponse;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
public class OpenAiLlmClient {

    private final WebClient webClient;
    private final AppProperties appProperties;

    public OpenAiLlmClient(WebClient.Builder webClientBuilder, AppProperties appProperties) {
        this.appProperties = appProperties;
        this.webClient = webClientBuilder
            .baseUrl(appProperties.getOpenai().getLlm().getUrl())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + appProperties.getOpenai().getApiKey())
            .build();
    }

    /**
     * Calls the OpenAI Chat Completions API with the given prompt.
     *
     * @param prompt The user prompt to send to the LLM.
     * @return The content of the first choice from the LLM response.
     */
    public Mono<String> call(String prompt) {
        log.info("Calling OpenAI API with model: {}", appProperties.getOpenai().getLlm().getModel());

        var openAiRequest = new OpenAiChatRequest(
            appProperties.getOpenai().getLlm().getModel(),
            List.of(new OpenAiChatMessage("user", prompt)),
            appProperties.getOpenai().getLlm().getTemperature(),
            appProperties.getOpenai().getLlm().getMaxTokens()
        );

        return webClient.post()
            .bodyValue(openAiRequest)
            .retrieve()
            .onStatus(
                status -> status.is4xxClientError() || status.is5xxServerError(),
                clientResponse -> clientResponse.bodyToMono(String.class)
                    .flatMap(errorBody -> {
                        log.error("Error response from OpenAI: {} {}", clientResponse.statusCode(), errorBody);
                        return Mono.error(new OpenAiApiException("API call failed with status " + clientResponse.statusCode()));
                    })
            )
            .bodyToMono(OpenAiChatResponse.class)
            .map(this::extractContent)
            .doOnError(e -> log.error("Unexpected error during OpenAI API call", e))
            .onErrorMap(e -> !(e instanceof OpenAiApiException), e -> new OpenAiApiException("An unexpected error occurred while calling OpenAI API.", e));
    }

    private String extractContent(OpenAiChatResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            log.error("Invalid response structure from OpenAI: {}", response);
            throw new OpenAiApiException("Received an invalid or empty response from OpenAI.");
        }
        
        OpenAiChatResponse.ResponseMessage message = response.choices().get(0).message();
        if (message == null || message.content() == null) {
            log.error("Missing response content from OpenAI.");
            throw new OpenAiApiException("Response content is missing from OpenAI.");
        }
        
        return message.content().trim();
    }

    // Custom exception for better error handling
    public static class OpenAiApiException extends RuntimeException {
        public OpenAiApiException(String message) {
            super(message);
        }

        public OpenAiApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
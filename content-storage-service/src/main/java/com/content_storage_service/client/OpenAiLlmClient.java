package com.content_storage_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.content_storage_service.client.OpenAiLlmClient.OpenAiApiException;
import com.content_storage_service.config.AppProperties;
import com.content_storage_service.dto.OpenAiChatMessage;
import com.content_storage_service.dto.OpenAiChatRequest;
import com.content_storage_service.dto.OpenAiChatResponse;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class OpenAiLlmClient {

    private final WebClient webClient;
    private final AppProperties appProperties;
    
    // Injected Config Values for Timeout and Retry
    private final long timeoutSeconds;
    private final int maxRetryAttempts;
    private final long minBackoffSeconds;

    public OpenAiLlmClient(WebClient.Builder webClientBuilder, AppProperties appProperties,
            @Value("${app.openai.llm.timeout-seconds}") long timeoutSeconds,
            @Value("${app.openai.llm.retry.max-attempts}") int maxRetryAttempts,
            @Value("${app.openai.llm.retry.min-backoff-seconds}") long minBackoffSeconds) {
        this.appProperties = appProperties;
        this.webClient = webClientBuilder
            .baseUrl(appProperties.getOpenai().getLlm().getUrl())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + appProperties.getOpenai().getApiKey())
            .build();
            
        this.timeoutSeconds = timeoutSeconds;
        this.maxRetryAttempts = maxRetryAttempts;
        this.minBackoffSeconds = minBackoffSeconds;
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
            List.of(new OpenAiChatMessage("user", prompt))
            //appProperties.getOpenai().getLlm().getTemperature(),
            //appProperties.getOpenai().getLlm().getMaxTokens()
        );

        return webClient.post()
            .bodyValue(openAiRequest)
            .retrieve()
            .onStatus(
                status -> status.is4xxClientError() || status.is5xxServerError(),
                clientResponse -> clientResponse.bodyToMono(String.class)
                    .flatMap(errorBody -> {
                        log.error("Error response from OpenAI: {} {}", clientResponse.statusCode(), errorBody);
                        return Mono.error(WebClientResponseException.create(
                            clientResponse.statusCode().value(),
                            "API call failed",
                            null,
                            errorBody.getBytes(),
                            null
                        ));
                    })
            )
            .bodyToMono(OpenAiChatResponse.class)
            .timeout(Duration.ofSeconds(this.timeoutSeconds))
            .retryWhen(Retry.backoff(this.maxRetryAttempts, Duration.ofSeconds(this.minBackoffSeconds))
                .filter(this::isTransientError) // Custom logic to decide which errors to retry
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                    new OpenAiApiException("OpenAI API call failed after " + retrySignal.totalRetries() + " retries.", retrySignal.failure())
                )
            )
            .map(this::extractContent)
            .doOnError(e -> log.error("Unexpected error during OpenAI API call", e))
            .onErrorMap(e -> !(e instanceof OpenAiApiException), e -> new OpenAiApiException("An unexpected error occurred while calling OpenAI API.", e));
    }

    /**
     * Determines if an error is transient and should be retried.
     * Retries on network issues, timeouts, rate limiting (429), and server errors (5xx).
     */
    private boolean isTransientError(Throwable throwable) {
        if (throwable instanceof IOException || throwable instanceof TimeoutException) {
            return true; // Network issues or our own timeout
        }
        if (throwable instanceof WebClientResponseException) {
            HttpStatusCode status = ((WebClientResponseException) throwable).getStatusCode();
            // Retry on "Too Many Requests" and all server-side errors
            return status.value() == 429 || status.is5xxServerError();
        }
        return false;
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
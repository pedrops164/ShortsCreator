package com.content_storage_service.service;

import com.content_storage_service.client.OpenAiLlmClient;
import com.content_storage_service.client.PaymentServiceClient;
import com.content_storage_service.dto.DialogueLine;
import com.content_storage_service.dto.GenerateTextRequest;
import com.content_storage_service.dto.GeneratedContent;
import com.content_storage_service.dto.GeneratedContentResponse;
import com.content_storage_service.enums.GenerationType;
import com.content_storage_service.exception.LlmResponseParsingException;
import com.content_storage_service.service.prompts.PromptStrategy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shortscreator.shared.dto.ChargeReasonV1;
import com.shortscreator.shared.dto.DebitRequestV1;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TextGenerationService {

    private final List<PromptStrategy> strategies;
    private final Map<GenerationType, PromptStrategy> strategyMap = new EnumMap<>(GenerationType.class);
    private final OpenAiLlmClient llmClient;
    private final PaymentServiceClient paymentServiceClient;
    private final Integer textGenerationCost;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public TextGenerationService(List<PromptStrategy> strategies, OpenAiLlmClient llmClient, PaymentServiceClient paymentServiceClient, @Value("${app.pricing.ai-text-generation-cost-usd}") Integer textGenerationCost) {
        this.strategies = strategies;
        this.llmClient = llmClient;
        this.paymentServiceClient = paymentServiceClient;
        this.textGenerationCost = textGenerationCost;
    }

    @PostConstruct
    public void initStrategies() {
        for (PromptStrategy strategy : strategies) {
            strategyMap.put(strategy.getGenerationType(), strategy);
        }
    }

    public Mono<GeneratedContentResponse> generateContent(GenerateTextRequest request, String userId) {
        PromptStrategy strategy = strategyMap.get(request.generationType());
        if (strategy == null) {
            return Mono.error(new IllegalArgumentException("No strategy found for generation type: " + request.generationType()));
        }

        String prompt = strategy.createPrompt(request.context());

        return llmClient.call(prompt)
            .flatMap(llmRawResponse -> {
                try {
                    // Parse the LLM response first
                    GeneratedContent content = parseLlmResponse(request.generationType(), llmRawResponse);
                    GeneratedContentResponse finalResponse = new GeneratedContentResponse(request.generationType(), content);

                    // Prepare the debit request
                    Map<String, String> metadata = Map.of(
                        "generationType", request.generationType().name(),
                        "responseCharacters", String.valueOf(llmRawResponse.length())
                    );
                    DebitRequestV1 debitRequest = new DebitRequestV1(
                        userId,
                        textGenerationCost,
                        ChargeReasonV1.AI_TEXT_GENERATION,
                        UUID.randomUUID().toString(), // Unique key for idempotency
                        metadata
                    );

                    log.info("AI text generated for user [{}]. Debiting account for {} USD.", userId, textGenerationCost);

                    // Call payment service and return the response ONLY on success
                    return paymentServiceClient.debitBalance(debitRequest)
                        .then(Mono.just(finalResponse)); // `.then()` ensures this runs after debitBalance completes

                } catch (JsonProcessingException e) {
                    log.error("Failed to parse LLM response for user [{}]. Raw response: {}", userId, llmRawResponse, e);
                    return Mono.error(new LlmResponseParsingException("Error parsing LLM response.", e));
                }
            });
    }

    // Helper to keep the reactive chain clean
    private GeneratedContent parseLlmResponse(GenerationType type, String rawResponse) throws JsonProcessingException {
        return switch (type) {
            case REDDIT_POST_DESCRIPTION -> new GeneratedContent(rawResponse, null, null);
            case REDDIT_COMMENT -> {
                List<String> comments = objectMapper.readValue(rawResponse, new TypeReference<>() {});
                yield new GeneratedContent(null, comments, null);
            }
            case CHARACTER_DIALOGUE -> {
                List<DialogueLine> dialogue = objectMapper.readValue(rawResponse, new TypeReference<>() {});
                yield new GeneratedContent(null, null, dialogue);
            }
        };
    }
}
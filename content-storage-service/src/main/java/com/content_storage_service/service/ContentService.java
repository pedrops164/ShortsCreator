package com.content_storage_service.service;

import com.content_storage_service.config.AppProperties;
import com.content_storage_service.model.Content;
import com.shortscreator.shared.enums.ContentType;
import com.shortscreator.shared.validation.TemplateValidator;
import com.shortscreator.shared.validation.TemplateValidator.ValidationException;
import com.shortscreator.shared.enums.ContentStatus;
import com.content_storage_service.repository.ContentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.shortscreator.shared.dto.GenerationRequestV1;
import com.shortscreator.shared.dto.OutputAssetsV1;
import com.shortscreator.shared.dto.StatusUpdateV1;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentService {

    private final ContentRepository contentRepository;
    private final TemplateValidator templateValidator; // Inject validator bean

    private final RabbitTemplate rabbitTemplate; // Inject RabbitTemplate
    private final AppProperties appProperties; // Inject the properties bean

    /**
     * Creates a new content draft.
     * @param userId The ID of the user creating the draft.
     * @param templateId The template chosen (e.g., "reddit_story_v1").
     * @param contentType The general type of content (e.g., REDDIT_STORY).
     * @param templateParams Initial parameters for the template.
     * @return A Mono emitting the created Content object.
     */
    public Mono<Content> createDraft(String userId, String templateId, ContentType contentType, JsonNode templateParams) {
        try {
            // The validator will now throw an exception on failure.
            templateValidator.validate(templateId, templateParams, false);
        } catch (ValidationException e) {
            return Mono.error(new IllegalArgumentException("Initial draft parameters are invalid: " + e.getMessage()));
        }
        // Generate a new UUID for the content ID
        String newContentId = UUID.randomUUID().toString();

        Content newContent = Content.builder()
                .id(newContentId)
                .userId(userId)
                .templateId(templateId)
                .contentType(contentType)
                .status(ContentStatus.DRAFT) // New content always starts as DRAFT
                .templateParams(templateParams)
                .build();

        return contentRepository.save(newContent);
    }

    /**
     * Updates an existing content draft.
     * @param contentId The ID of the content to update.
     * @param userId The ID of the user (for security/ownership check).
     * @param updatedTemplateParams The updated template parameters.
     * @return A Mono emitting the updated Content object, or an error if not found or unauthorized.
     */
    public Mono<Content> updateDraft(String contentId, String userId, JsonNode updatedTemplateParams) {
        return contentRepository.findByIdAndUserId(contentId, userId)
                .flatMap(existingContent -> {
                    if (existingContent.getStatus() != ContentStatus.DRAFT) {
                        return Mono.error(new IllegalStateException("Cannot update a content item that is not a DRAFT. Current status: " + existingContent.getStatus()));
                    }

                    // Perform JSON Schema validation on the updatedTemplateParams
                    try {
                        templateValidator.validate(existingContent.getTemplateId(), updatedTemplateParams, false);
                        existingContent.setTemplateParams(updatedTemplateParams);
                        return contentRepository.save(existingContent);
                    } catch (ValidationException e) {
                        return Mono.error(new IllegalArgumentException("Updated draft parameters are invalid: " + e.getMessage()));
                    }
                })
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Content not found or unauthorized for ID: " + contentId)));
    }

    /**
     * Retrieves all drafts for a specific user.
     * @param userId The ID of the user.
     * @return A Flux emitting Content objects with DRAFT status.
     */
    public Flux<Content> getUserDrafts(String userId) {
        return contentRepository.findByUserIdAndStatus(userId, ContentStatus.DRAFT);
    }

    /**
     * Retrieves a specific content item by its ID and user ID.
     * @param contentId The ID of the content.
     * @param userId The ID of the user (for security/ownership check).
     * @return A Mono emitting the Content object, or empty if not found or unauthorized.
     */
    public Mono<Content> getContentByIdAndUserId(String contentId, String userId) {
        return contentRepository.findByIdAndUserId(contentId, userId);
    }

    /**
     * Submits a completed draft for content generation.
     * This will update its status to PROCESSING and notify the Content Generation Service.
     * @param contentId The ID of the content to submit.
     * @param userId The ID of the user (for security/ownership check).
     * @return A Mono emitting the updated Content object.
     */
    public Mono<Content> submitForGeneration(String contentId, String userId) {
        return contentRepository.findByIdAndUserId(contentId, userId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Draft not found")))
                .flatMap(content -> {
                    if (content.getStatus() != ContentStatus.DRAFT) {
                        return Mono.error(new IllegalStateException("Only DRAFTs can be submitted for generation. Current status: " + content.getStatus()));
                    }

                    try {
                        // Use the validator for final validation
                        templateValidator.validate(content.getTemplateId(), content.getTemplateParams(), true);
                    } catch (ValidationException e) {
                        content.setStatus(ContentStatus.FAILED);
                        content.setErrorMessage("Final validation failed: " + e.getMessage());
                        return contentRepository.save(content)
                            .then(Mono.error(new IllegalArgumentException("Draft is not valid for generation.")));
                    }
                    content.setStatus(ContentStatus.PROCESSING);
                    // Save the content with updated status, and send a message to the Content Generation Service
                    return contentRepository.save(content)
                            .doOnSuccess(processingContent -> {
                                // 3. Create the DTO for the message
                                GenerationRequestV1 request = new GenerationRequestV1(
                                    processingContent.getId(),
                                    processingContent.getUserId(),
                                    processingContent.getTemplateId(),
                                    processingContent.getTemplateParams()
                                );

                                // 4. Construct routing key and send message
                                String routingKey = appProperties.getRabbitmq().getRoutingKeys().getGenerationRequestPrefix() + processingContent.getTemplateId(); // e.g., "request.generate.reddit_story_v1"
                                String exchangeName = appProperties.getRabbitmq().getExchange(); // e.g., "content_generation_exchange"
                                rabbitTemplate.convertAndSend(exchangeName, routingKey, request);
                                log.info("Sent generation request for contentId: {} with routingKey: {}", processingContent.getId(), routingKey);
                            });
                })
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Content not found or unauthorized for ID: " + contentId)));
    }

    public Mono<Content> processStatusUpdate(StatusUpdateV1 update) {
        return contentRepository.findById(update.getContentId())
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Content not found for status update")))
            .flatMap(content -> {
                ContentStatus newStatus = update.getStatus();

                // Idempotency check: Don't re-process a terminal state.
                if (content.getStatus() == ContentStatus.COMPLETED || content.getStatus() == ContentStatus.FAILED) {
                    log.warn("Content {} is already in a terminal state ({}). Ignoring update.", content.getId(), content.getStatus());
                    return Mono.just(content);
                }

                content.setStatus(newStatus);
                if (newStatus == ContentStatus.COMPLETED) {
                    // Convert JsonNode to your OutputAssets POJO
                    OutputAssetsV1 assets = update.getOutputAssets();
                    content.setOutputAssets(assets);
                    content.setErrorMessage(null);
                } else if (newStatus == ContentStatus.FAILED) {
                    content.setErrorMessage(update.getErrorMessage());
                }

                return contentRepository.save(content);
            });
    }
}
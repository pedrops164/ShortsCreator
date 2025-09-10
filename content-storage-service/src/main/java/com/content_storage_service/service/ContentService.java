package com.content_storage_service.service;

import com.content_storage_service.client.PaymentServiceClient;
import com.content_storage_service.config.AppProperties;
import com.content_storage_service.model.Content;
import com.shortscreator.shared.enums.ContentType;
import com.shortscreator.shared.validation.TemplateValidator;
import com.shortscreator.shared.validation.TemplateValidator.ValidationException;
import com.shortscreator.shared.enums.ContentStatus;
import com.content_storage_service.repository.ContentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.shortscreator.shared.dto.ContentPriceV1;
import com.shortscreator.shared.dto.GeneratedVideoDetailsV1;
import com.shortscreator.shared.dto.GenerationRequestV1;
import com.shortscreator.shared.dto.GenerationResultV1;
import com.shortscreator.shared.dto.OutputAssetsV1;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentService {

    private final ContentRepository contentRepository;
    private final TemplateValidator templateValidator;

    private final RabbitTemplate rabbitTemplate;
    private final AppProperties appProperties;
    private final Map<String, ContentType> templateToContentTypeMap;
    private final PriceCalculationService priceCalculationService;
    private final PaymentServiceClient paymentServiceClient;
    private final DownloadService downloadService;

    /**
     * Creates a new content draft.
     * @param userId The ID of the user creating the draft.
     * @param templateId The template chosen (e.g., "reddit_story_v1").
     * @param contentType The general type of content (e.g., REDDIT_STORY).
     * @param templateParams Initial parameters for the template.
     * @return A Mono emitting the created Content object.
     */
    public Mono<Content> createDraft(String userId, String templateId, JsonNode templateParams) {
        log.info("Attempting to create a new draft for user [{}] with template [{}]", userId, templateId);
        // Infer ContentType from templateId using the injected map
        ContentType contentType = inferContentTypeFromTemplateId(templateId);
        try {
            log.debug("Validating initial draft parameters for template [{}]", templateId);
            templateValidator.validate(templateId, templateParams, false);
        } catch (ValidationException e) {
            log.warn("Initial draft validation failed for user [{}]: {}", userId, e.getMessage());
            return Mono.error(new IllegalArgumentException("Initial draft parameters are invalid: " + e.getMessage()));
        }

        Content newContent = Content.builder()
                .userId(userId)
                .templateId(templateId)
                .contentType(contentType)
                .status(ContentStatus.DRAFT) // New content always starts as DRAFT
                .templateParams(templateParams)
                .build();

        return contentRepository.save(newContent)
                .doOnSuccess(savedContent -> log.info("Successfully created draft with ID [{}] for user [{}]", savedContent.getId(), userId));
    }

    /**
     * Updates an existing content draft.
     * @param contentId The ID of the content to update.
     * @param userId The ID of the user (for security/ownership check).
     * @param updatedTemplateParams The updated template parameters.
     * @return A Mono emitting the updated Content object, or an error if not found or unauthorized.
     */
    public Mono<Content> updateDraft(String contentId, String userId, JsonNode updatedTemplateParams) {
        log.info("User [{}] is attempting to update draft [{}]", userId, contentId);
        return contentRepository.findByIdAndUserId(contentId, userId)
                .flatMap(existingContent -> {
                    log.debug("Found draft [{}] for update. Current status: {}", contentId, existingContent.getStatus());
                    if (existingContent.getStatus() != ContentStatus.DRAFT && existingContent.getStatus() != ContentStatus.FAILED) {
                        log.warn("User [{}] attempted to update content [{}] whose state isnt DRAFT nor FAILED (status: {})", userId, contentId, existingContent.getStatus());
                        return Mono.error(new IllegalStateException("Cannot update a content item whose state isnt DRAFT nor FAILED. Current status: " + existingContent.getStatus()));
                    }

                    existingContent.setStatus(ContentStatus.DRAFT); // Ensure status is DRAFT for updates
                    // Perform JSON Schema validation on the updatedTemplateParams
                    try {
                        log.debug("Validating updated draft parameters for content [{}]", contentId);
                        templateValidator.validate(existingContent.getTemplateId(), updatedTemplateParams, false);
                        existingContent.setTemplateParams(updatedTemplateParams);
                        return contentRepository.save(existingContent)
                                .doOnSuccess(saved -> log.info("Successfully updated draft [{}]", saved.getId()));
                    } catch (ValidationException e) {
                        log.warn("Updated draft validation failed for content [{}]: {}", contentId, e.getMessage());
                        return Mono.error(new IllegalArgumentException("Updated draft parameters are invalid: " + e.getMessage()));
                    }
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Update failed: Content [{}] not found or does not belong to user [{}]", contentId, userId);
                    return Mono.error(new IllegalArgumentException("Content not found or unauthorized for ID: " + contentId));
                }));
    }

    /**
     * Retrieves all content for a specific user, optionally filtered by statuses.
     * @param userId The ID of the user.
     * @param statuses Optional list of ContentStatus to filter by.
     * @return A Flux emitting Content objects.
     */
    public Flux<Content> getUserContentByStatus(String userId, List<ContentStatus> statuses) {
    // If no statuses are provided, return all content for the user.
    // Otherwise, filter by the list of statuses.
    log.info("Fetching content for user [{}] with statuses [{}]", userId, statuses);
    if (statuses == null || statuses.isEmpty()) {
        return contentRepository.findByUserIdOrderByLastModifiedAtDesc(userId);
    } else {
        // This requires a new method in your Spring Data repository interface
        return contentRepository.findByUserIdAndStatusInOrderByLastModifiedAtDesc(userId, statuses);
    }
}

    /**
     * Retrieves all content for a specific user.
     * @param userId The ID of the user.
     * @return A Flux emitting Content objects.
     */
    public Flux<Content> getUserContent(String userId) {
        log.info("Fetching all drafts for user [{}]", userId);
        return contentRepository.findByUserIdOrderByLastModifiedAtDesc(userId);
    }

    /**
     * Retrieves a specific content item by its ID and user ID.
     * @param contentId The ID of the content.
     * @param userId The ID of the user (for security/ownership check).
     * @return A Mono emitting the Content object, or empty if not found or unauthorized.
     */
    public Mono<Content> getContentByIdAndUserId(String contentId, String userId) {
        log.info("Fetching content [{}] for user [{}]", contentId, userId);
        return contentRepository.findByIdAndUserId(contentId, userId)
                .doOnSuccess(content -> {
                    if (content != null) {
                        log.debug("Found content [{}] for user [{}]", contentId, userId);
                    } else {
                        log.debug("No content found with ID [{}] for user [{}]", contentId, userId);
                    }
                });
    }

    /**
     * Submits a completed draft for content generation.
     * This will update its status to PROCESSING and notify the Content Generation Service.
     * @param contentId The ID of the content to submit.
     * @param userId The ID of the user (for security/ownership check).
     * @return A Mono emitting the updated Content object.
     */
    public Mono<Content> submitForGeneration(String contentId, String userId) {
        log.info("User [{}] attempting to submit content [{}] for generation", userId, contentId);

        // Fetch the content draft
        Mono<Content> contentMono = contentRepository.findByIdAndUserId(contentId, userId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Draft not found or unauthorized")));

        return contentMono.flatMap(content -> {
            if (content.getStatus() != ContentStatus.DRAFT) {
                log.warn("Submission failed: Content [{}] for user [{}] is not a DRAFT (status: {})", contentId, userId, content.getStatus());
                return Mono.error(new IllegalStateException("Only DRAFTs can be submitted for generation. Current status: " + content.getStatus()));
            }

            try {
                // Perform final validation
                log.debug("Performing final validation for content [{}]", contentId);
                templateValidator.validate(content.getTemplateId(), content.getTemplateParams(), true);
            } catch (ValidationException e) {
                log.error("Final validation failed for content [{}]. Reason: {}", contentId, e.getMessage());
                return Mono.error(new IllegalArgumentException("Draft is not valid for generation."));
            }

            // Calculate the Price to generate the draft
            log.info("Content [{}] passed validation. Calculating price.", contentId);
            ContentPriceV1 priceResponse;
            try {
                priceResponse = priceCalculationService.calculatePrice(content);
            } catch (Exception e) {
                return Mono.error(new IllegalStateException("Failed to calculate price: " + e.getMessage()));
            }
            log.info("Calculated price for content [{}] is {} cents. Currency: {}", contentId, priceResponse.finalPrice(), priceResponse.currency());

            // Debit the User's Account
            return paymentServiceClient.debitForGeneration(userId, contentId, priceResponse, content.getContentType())
                    .then(Mono.just(content)); // Pass the 'content' object down the chain on success
        })
        .flatMap(content -> {
            // Queue for Generation (This block only runs if payment was successful)
            content.setStatus(ContentStatus.PROCESSING);
            log.info("Debit successful. Setting content [{}] status to PROCESSING.", content.getId());
            return contentRepository.save(content)
                .doOnSuccess(processingContent -> {
                    GenerationRequestV1 request = new GenerationRequestV1(
                        processingContent.getId(),
                        processingContent.getUserId(),
                        processingContent.getTemplateId(),
                        processingContent.getTemplateParams()
                    );
                    String routingKey = appProperties.getRabbitmq().getRoutingKeys().getGenerationRequestPrefix() + processingContent.getTemplateId();
                    String exchangeName = appProperties.getRabbitmq().getExchange();
                    rabbitTemplate.convertAndSend(exchangeName, routingKey, request);
                    log.info("Sent generation request for contentId [{}] with routingKey: {}", processingContent.getId(), routingKey);
                });
        });
    }

    /**
     * Processes the result of a content generation request.
     * This updates the content status based on the GenerationResultV1 received.
     * @param generationResult The result of the content generation.
     * @return A Mono emitting the updated Content object.
     */
    public Mono<Content> processGenerationResult(GenerationResultV1 generationResult) {
        log.info("Processing status update for content [{}]. New status: {}", generationResult.getContentId(), generationResult.getStatus());
        return contentRepository.findById(generationResult.getContentId())
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Content not found for status update")))
            .flatMap(content -> {
                ContentStatus newStatus = generationResult.getStatus();

                // Idempotency check: Don't re-process a terminal state.
                if (content.getStatus() == ContentStatus.COMPLETED || content.getStatus() == ContentStatus.FAILED) {
                    log.warn("Content {} is already in a terminal state ({}). Ignoring update.", content.getId(), content.getStatus());
                    return Mono.just(content);
                }

                content.setStatus(newStatus);
                if (newStatus == ContentStatus.COMPLETED) {
                    log.info("Content [{}] has been successfully COMPLETED.", content.getId());
                    GeneratedVideoDetailsV1 details = generationResult.getGeneratedVideoDetails();
                    OutputAssetsV1 outputAssets = new OutputAssetsV1(
                        details.getS3Url(),
                        details.getS3Key(),
                        details.getDurationSeconds() // Use the real duration
                    );
                    content.setOutputAssets(outputAssets);
                } else if (newStatus == ContentStatus.FAILED) {
                    log.error("Content [{}] has FAILED processing. Reason: {}", content.getId(), generationResult.getErrorMessage());
                    content.setErrorMessage(generationResult.getErrorMessage());
                    // Refund the user if the content generation failed
                    paymentServiceClient.requestRefund(content.getId())
                            .doOnError(e -> log.error("CRITICAL: Refund request failed for contentId {}: {}", content.getId(), e.getMessage()))
                            .doOnSuccess(s -> log.info("Refund request successfully sent for contentId {}", content.getId()))
                            .subscribe(); // Fire-and-forget the refund call
                }
                
                return contentRepository.save(content)
                .doOnSuccess(saved -> log.info("Successfully updated status for content [{}] to {}", saved.getId(), saved.getStatus()));

            });
    }

    /**
     * Helper method to infer ContentType from templateId using the injected map.
     */
    private ContentType inferContentTypeFromTemplateId(String templateId) {
        return templateToContentTypeMap.get(templateId);
    }

    // Delete content
    public Mono<Void> deleteContent(String contentId, String userId) {
        log.info("User [{}] is attempting to delete content [{}]", userId, contentId);
        // First, verify that the content exists and belongs to the user
        return contentRepository.findByIdAndUserId(contentId, userId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found or does not belong to user")))
                .flatMap(content -> contentRepository.delete(content))
                .then(); // Convert Mono<Content> from delete(content) to Mono<Void>
    }

    public Mono<ContentPriceV1> calculateDraftPrice(String contentId, String userId) {
        return contentRepository.findByIdAndUserId(contentId, userId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Content not found with ID: " + contentId)))
                .flatMap(content -> {
                    if (content.getStatus() != ContentStatus.DRAFT) {
                        return Mono.error(new IllegalStateException("Price can only be calculated for drafts. Current status: " + content.getStatus()));
                    }
                    try {
                        ContentPriceV1 priceResponse = priceCalculationService.calculatePrice(content);
                        return Mono.just(priceResponse);
                    } catch (IllegalArgumentException e) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage()));
                    }
                });
    }

    /**
     * Gets the download URL for a content draft.
     *
     * @param contentId The ID of the content draft.
     * @param userId The ID of the user owning the draft.
     * @return A String representing the download URL.
     */
    public Mono<String> getDownloadUrl(String contentId, String userId) {
        return contentRepository.findById(contentId)
            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found")))
            .flatMap(content -> {
                if (content.getStatus() != ContentStatus.COMPLETED || content.getOutputAssets() == null) {
                    return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Video is not available for download"));
                }
                // Delegate the actual URL generation to the injected service
                return downloadService.getDownloadUrl(content);
            });
    }
}
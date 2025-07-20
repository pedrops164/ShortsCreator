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
import com.shortscreator.shared.dto.GenerationResultV1;
import com.shortscreator.shared.dto.OutputAssetsV1;
import com.shortscreator.shared.dto.VideoUploadJobV1;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map; // Import Map
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
    private final VideoUploadProcessorService processorService;
    private final Map<String, ContentType> templateToContentTypeMap; // Inject the map

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
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Content not found or unauthorized for ID: " + contentId)));
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
        return contentRepository.findByIdAndUserId(contentId, userId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Draft not found")))
                .flatMap(content -> {
                    if (content.getStatus() != ContentStatus.DRAFT) {
                        log.warn("Submission failed: Content [{}] for user [{}] is not a DRAFT (status: {})", contentId, userId, content.getStatus());
                        return Mono.error(new IllegalStateException("Only DRAFTs can be submitted for generation. Current status: " + content.getStatus()));
                    }

                    try {
                        log.debug("Performing final validation for content [{}]", contentId);
                        templateValidator.validate(content.getTemplateId(), content.getTemplateParams(), true);
                    } catch (ValidationException e) {
                        log.error("Final validation failed for content [{}]. Setting status to FAILED. Reason: {}", contentId, e.getMessage());
                        content.setStatus(ContentStatus.FAILED);
                        content.setErrorMessage("Final validation failed: " + e.getMessage());
                        return contentRepository.save(content)
                            .then(Mono.error(new IllegalArgumentException("Draft is not valid for generation.")));
                    }
                    content.setStatus(ContentStatus.PROCESSING);
                    log.info("Content [{}] passed final validation. Setting status to PROCESSING.", contentId);
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
                                log.debug("Preparing to send generation request for content [{}]. Routing Key: {}", processingContent.getId(), routingKey);
                                rabbitTemplate.convertAndSend(exchangeName, routingKey, request);
                                log.info("Sent generation request for contentId [{}] with routingKey: {}", processingContent.getId(), routingKey);
                            });
                })
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Content not found or unauthorized for ID: " + contentId)));
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
                    VideoUploadJobV1 job = generationResult.getVideoUploadJobV1();
                    String finalUrl = processorService.processUploadJob(job);
                    OutputAssetsV1 outputAssets = new OutputAssetsV1(
                        finalUrl,
                        60 // Example duration in seconds
                    );
                    content.setOutputAssets(outputAssets);
                } else if (newStatus == ContentStatus.FAILED) {
                    log.error("Content [{}] has FAILED processing. Reason: {}", content.getId(), generationResult.getErrorMessage());
                    content.setErrorMessage(generationResult.getErrorMessage());
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

    // New method to delete content
    public Mono<Void> deleteContent(String contentId, String userId) {
        // First, verify that the content exists and belongs to the user
        return contentRepository.findByIdAndUserId(contentId, userId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found or does not belong to user")))
                .flatMap(content -> contentRepository.delete(content))
                .then(); // Convert Mono<Content> from delete(content) to Mono<Void>
    }
}
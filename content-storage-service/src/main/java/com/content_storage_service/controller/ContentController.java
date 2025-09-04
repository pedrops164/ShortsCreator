package com.content_storage_service.controller;

import com.content_storage_service.model.Content;
import com.content_storage_service.service.ContentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.shortscreator.shared.dto.ContentPriceV1;
import com.shortscreator.shared.enums.ContentStatus;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import com.content_storage_service.dto.ContentCreationRequest; // DTO for creating a new content draft

import org.springframework.web.server.ResponseStatusException; // For throwing HTTP errors

@RestController
@RequestMapping("/api/v1/content")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    // Endpoint for a user to start a new content draft
    @PostMapping("/drafts")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Content> createContentDraft(@RequestBody ContentCreationRequest request, @RequestHeader("X-User-ID") String userId) {
        return contentService.createDraft(userId, request.getTemplateId(), request.getTemplateParams());
    }

    // Endpoint for a user to update existing content
    @PutMapping("/{contentId}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<Content> updateContentDraft(@PathVariable String contentId, @RequestBody JsonNode updatedTemplateParams, @RequestHeader("X-User-ID") String userId) {
        return contentService.updateDraft(contentId, userId, updatedTemplateParams);
    }

    // Endpoint to get all content for the authenticated user
    @GetMapping()
    @ResponseStatus(HttpStatus.OK)
    public Flux<Content> getUserContent(
            @RequestHeader("X-User-ID") String userId,
            @RequestParam(required = false) List<ContentStatus> statuses) {
        return contentService.getUserContentByStatus(userId, statuses);
    }

    // Endpoint to get a specific content item (draft or completed) by ID
    @GetMapping("/{contentId}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<Content> getContentById(@PathVariable String contentId, @RequestHeader("X-User-ID") String userId) {
        return contentService.getContentByIdAndUserId(contentId, userId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found.")));
    }

    // Endpoint for a user to submit a draft for generation
    @PostMapping("/{contentId}/generate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Content> submitContentForGeneration(@PathVariable String contentId, @RequestHeader("X-User-ID") String userId) {
        return contentService.submitForGeneration(contentId, userId);
    }

    // New endpoint to delete content
    @DeleteMapping("/{contentId}") // Path variable for the content ID
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204 No Content is standard for successful DELETE
    public Mono<Void> deleteContent(
            @PathVariable String contentId,
            @RequestHeader("X-User-ID") String userId) {
        return contentService.deleteContent(contentId, userId);
    }

    /**
     * Calculates and returns the generation price for a given content draft.
     * @param contentId The ID of the content draft.
     * @param userId The ID of the user owning the draft.
     * @return A Mono containing the price details.
     */
    @GetMapping("/{contentId}/price")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ContentPriceV1> getDraftPrice(
            @PathVariable String contentId,
            @RequestHeader("X-User-ID") String userId) {
        return contentService.calculateDraftPrice(contentId, userId);
    }
}
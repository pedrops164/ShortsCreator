package com.content_storage_service.controller;

import com.content_storage_service.model.Content;
import com.content_storage_service.service.ContentService;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

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
        return contentService.createDraft(userId, request.getTemplateId(), request.getTemplateParams())
                .onErrorResume(e -> Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage())));
    }

    // Endpoint for a user to update existing content
    @PutMapping("/{contentId}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<Content> updateContentDraft(@PathVariable String contentId, @RequestBody JsonNode updatedTemplateParams, @RequestHeader("X-User-ID") String userId) {
        return contentService.updateDraft(contentId, userId, updatedTemplateParams)
                .onErrorResume(IllegalStateException.class, e -> Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage())))
                .onErrorResume(IllegalArgumentException.class, e -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage())))
                .onErrorResume(e -> Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage())));
    }

    // Endpoint to get all drafts for the authenticated user
    @GetMapping("/drafts")
    @ResponseStatus(HttpStatus.OK)
    public Flux<Content> getUserDrafts(@RequestHeader("X-User-ID") String userId) {
        return contentService.getUserDrafts(userId);
    }

    // Endpoint to get all content for the authenticated user
    @GetMapping("/content")
    @ResponseStatus(HttpStatus.OK)
    public Flux<Content> getUserContent(@RequestHeader("X-User-ID") String userId) {
        return contentService.getUserContent(userId);
    }

    // Endpoint to get a specific content item (draft or completed) by ID
    @GetMapping("/{contentId}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<Content> getContentById(@PathVariable String contentId, @RequestHeader("X-User-ID") String userId) {
        return contentService.getContentByIdAndUserId(contentId, userId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found.")))
                .onErrorResume(e -> Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving content.", e)));
    }

    // Endpoint for a user to submit a draft for generation
    @PostMapping("/{contentId}/generate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Content> submitContentForGeneration(@PathVariable String contentId, @RequestHeader("X-User-ID") String userId) {
        return contentService.submitForGeneration(contentId, userId)
                .onErrorResume(IllegalStateException.class, e -> Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage())))
                .onErrorResume(IllegalArgumentException.class, e -> Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage())));
                //.onErrorResume(e -> Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error submitting content for generation.", e)));
    }
}
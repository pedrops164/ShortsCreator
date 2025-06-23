package com.content_storage_service.controller;

import com.content_storage_service.model.Content;
import com.content_storage_service.service.ContentService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import com.content_storage_service.dto.ContentCreationRequest; // DTO for creating a new content draft

import org.springframework.web.server.ResponseStatusException; // For throwing HTTP errors

import java.security.Principal; // To get the authenticated user ID (from OAuth2 provider)

@RestController
@RequestMapping("/api/v1/content")
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    // Endpoint for a user to start a new content draft
    @PostMapping("/drafts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public Mono<Content> createContentDraft(@RequestBody ContentCreationRequest request, Principal principal) {
        // In a real application, 'principal' would be used to get the actual authenticated user ID.
        // For now, let's assume request.userId is validated against principal.getName() in a real auth setup.
        String userId = request.getUserId(); // Or principal.getName() for authenticated user
        return contentService.createDraft(userId, request.getTemplateId(), request.getContentType(), request.getTemplateParams())
                .onErrorResume(e -> Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage())));
    }

    // Endpoint for a user to update an existing draft
    @PutMapping("/drafts/{contentId}")
    @PreAuthorize("@contentSecurity.isOwner(#contentId, principal)") 
    public Mono<Content> updateContentDraft(@PathVariable String contentId, @RequestBody JsonNode updatedTemplateParams, Principal principal) {
        String userId = principal.getName(); // Get authenticated user ID
        return contentService.updateDraft(contentId, userId, updatedTemplateParams)
                .onErrorResume(IllegalStateException.class, e -> Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage())))
                .onErrorResume(IllegalArgumentException.class, e -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage())))
                .onErrorResume(e -> Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage())));
    }

    // Endpoint to get all drafts for the authenticated user
    @GetMapping("/drafts")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("isAuthenticated()") // Ensure the user is authenticated
    public Flux<Content> getUserDrafts(Principal principal) {
        String userId = principal.getName(); // Get authenticated user ID
        return contentService.getUserDrafts(userId);
    }

    // Endpoint to get a specific content item (draft or completed) by ID
    @GetMapping("/{contentId}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("@contentSecurity.isOwner(#contentId, principal)") 
    public Mono<Content> getContentById(@PathVariable String contentId, Principal principal) {
        String userId = principal.getName(); // Get authenticated user ID
        return contentService.getContentByIdAndUserId(contentId, userId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found.")))
                .onErrorResume(e -> Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving content.", e)));
    }

    // Endpoint for a user to submit a draft for generation
    @PostMapping("/{contentId}/generate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("@contentSecurity.isOwner(#contentId, authentication)") // Ensure the user is the owner of the content
    public Mono<Content> submitContentForGeneration(@PathVariable String contentId, Principal principal) {
        String userId = principal.getName(); // Get authenticated user ID
        return contentService.submitForGeneration(contentId, userId)
                .onErrorResume(IllegalStateException.class, e -> Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage())))
                .onErrorResume(IllegalArgumentException.class, e -> Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage())))
                .onErrorResume(e -> Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error submitting content for generation.", e)));
    }
}
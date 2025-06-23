package com.content_storage_service.service;

import com.content_storage_service.repository.ContentRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Service for handling content security checks.
 * This service is used to determine if the currently authenticated user is the owner of a specific content item.
 */
@Service("contentSecurity") // The name "contentSecurity" matches the @PreAuthorize expression
public class ContentSecurity {

    private final ContentRepository contentRepository;

    public ContentSecurity(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    /**
     * Checks if the currently authenticated user is the owner of the content.
     */
    public Mono<Boolean> isOwner(String contentId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Mono.just(false);
        }
        // Authentication.getName() gives you the username
        String userId = authentication.getName();
        return contentRepository.findById(contentId)
            .map(content -> content.getUserId().equals(userId))
            .defaultIfEmpty(false);
    }
}
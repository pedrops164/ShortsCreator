package com.content_storage_service.service;

import com.content_storage_service.repository.ContentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Service for handling content security checks.
 * This service is used to determine if the currently authenticated user is the owner of a specific content item.
 */
@Slf4j
@Service("contentSecurity") // The name "contentSecurity" matches the @PreAuthorize expression
@RequiredArgsConstructor
public class ContentSecurity {

    private final ContentRepository contentRepository;

    /**
     * Checks if the currently authenticated user is the owner of the content.
     * 
     * @param contentId The ID of the content to check ownership for.
     * @param authentication The current authentication object, which contains user details.
     * @return A Mono that emits true if the user is the owner, false otherwise.
     */
    public Mono<Boolean> isOwner(String contentId, Authentication authentication) {
        log.trace("Executing isOwner check for contentId [{}] and principal [{}]", 
                  contentId, (authentication != null ? authentication.getName() : "null"));
        if (authentication == null || !authentication.isAuthenticated()) {
            log.info("isOwner check failed: user is not authenticated for contentId [{}]", contentId);
            return Mono.just(false);
        }
        // Authentication.getName() gives you the username
        String userId = authentication.getName();
        return contentRepository.findById(contentId)
            .map(content -> content.getUserId().equals(userId))
            .defaultIfEmpty(false)
            .doOnSuccess(isOwner -> {
                if (!isOwner) {
                    log.info("Authorization FAILED for principal [{}] on contentId [{}]", userId, contentId);
                } else {
                    log.info("Authorization SUCCEEDED for principal [{}] on contentId [{}]", userId, contentId);
                }
            });
    }
}
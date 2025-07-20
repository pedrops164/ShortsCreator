package com.content_storage_service.repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import com.content_storage_service.model.Content;
import com.shortscreator.shared.enums.ContentStatus;

public interface ContentRepository extends ReactiveMongoRepository<Content, String> {

    // Custom query to find content by userId and status (get drafts of a user)
    Flux<Content> findByUserIdAndStatus(String userId, ContentStatus status);

    // Custom query to find a specific content object by content id and user id
    Mono<Content> findByIdAndUserId(String id, String userId);

    // Custom query to find a specific content object by user id
    Flux<Content> findByUserIdOrderByLastModifiedAtDesc(String userId);

    Flux<Content> findByUserIdAndStatusInOrderByLastModifiedAtDesc(String userId, Collection<ContentStatus> statuses);
}

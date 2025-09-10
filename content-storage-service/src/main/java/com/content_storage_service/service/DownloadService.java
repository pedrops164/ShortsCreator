package com.content_storage_service.service;

import com.content_storage_service.model.Content;
import reactor.core.publisher.Mono;

public interface DownloadService {
    /**
     * Generates a publicly accessible, temporary URL for downloading the given content.
     *
     * @param content The fully validated and authorized Content object.
     * @return A Mono emitting the download URL as a String.
     */
    Mono<String> getDownloadUrl(Content content);
}
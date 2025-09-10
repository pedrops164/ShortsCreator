package com.content_storage_service.service;

import com.content_storage_service.model.Content;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Profile("dev") // Only active in the 'dev' environment
public class LocalDownloadService implements DownloadService {

    @Override
    public Mono<String> getDownloadUrl(Content content) {
        // In dev mode, we don't connect to S3. Return an informative error.
        return Mono.error(new UnsupportedOperationException("Download functionality is not available in the 'dev' profile."));
    }
}
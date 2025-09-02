package com.content_storage_service.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;

@Slf4j
@Service
public class LocalFileStorageService implements StorageService {

    private final String baseUrl;

    /* public LocalFileStorageService(
        @Value("${app.storage.local-permanent.base-url}") String baseUrl
    ) {
        this.baseUrl = baseUrl;
    } */

    public LocalFileStorageService() {
        this.baseUrl = "http://localhost:8080/files/"; // Dummy base URL for local storage
    }

    @Override
    public String store(Path sourceFile, String destinationKey) throws IOException {
        // Construct the URL
        String finalUrl = this.baseUrl + destinationKey;
        log.info("Generated dummy URL: {}", finalUrl);
        return finalUrl;
    }
}
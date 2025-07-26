package com.content_generation_service.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * A utility service for handling file and resource operations,
 * such as loading files from the classpath.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceHelperService {

    private final ResourceLoader resourceLoader;

    /**
     * Loads a resource from the classpath and copies it to a temporary file on the filesystem.
     * This is necessary when an API client requires a java.io.File or Path object for a file
     * that is bundled inside the application JAR.
     *
     * @param resourcePath The full path to the classpath resource (e.g., "classpath:assets/audio/peter.mp3").
     * @return The Path to the newly created temporary file.
     * @throws IOException if the resource cannot be found or copied.
     */
    public Path createTempFileFromClasspath(String resourcePath) throws IOException {
        log.debug("Loading classpath resource: {}", resourcePath);
        Resource resource = resourceLoader.getResource(resourcePath);
        if (!resource.exists()) {
            throw new IOException("Resource not found: " + resourcePath);
        }

        // Get the input stream from the resource inside the JAR/classpath
        try (InputStream inputStream = resource.getInputStream()) {
            // Create a temporary file on the filesystem
            Path tempFile = Files.createTempFile("resource-sample-", ".tmp");
            
            // Copy the resource stream to the temporary file
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Copied classpath resource '{}' to temporary file: {}", resourcePath, tempFile);
            return tempFile;
        }
    }

    /**
     * Safely deletes a temporary file and logs a warning on failure.
     * @param path The path to the file to be deleted.
     */
    public void deleteTemporaryFile(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete temporary audio file: {}", path, e);
        }
    }
}
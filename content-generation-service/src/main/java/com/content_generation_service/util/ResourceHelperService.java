package com.content_generation_service.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A utility service for handling file and resource operations,
 * such as loading files from the classpath.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceHelperService {

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
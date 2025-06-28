package com.content_storage_service.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
@Service
@Profile("dev") // This bean is only active when the 'dev' profile is used
public class LocalFileStorageService implements StorageService {

    private final Path permanentStorageLocation;
    private final String baseUrl;

    public LocalFileStorageService(
        @Value("${app.storage.local-permanent.location}") String location,
        @Value("${app.storage.local-permanent.base-url}") String baseUrl
    ) {
        this.permanentStorageLocation = Paths.get(location).toAbsolutePath().normalize();
        this.baseUrl = baseUrl;
        try {
            Files.createDirectories(this.permanentStorageLocation);
            log.info("Permanent local storage directory created at: {}", this.permanentStorageLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not create permanent local storage directory", e);
        }
    }

    @Override
    public String store(Path sourceFile, String destinationKey) throws IOException {
        // Normalize the destination path to prevent directory traversal issues
        Path destinationFile = this.permanentStorageLocation.resolve(destinationKey).toAbsolutePath().normalize();

        // Security check: Ensure the destination is within the intended storage location
        if (!destinationFile.startsWith(this.permanentStorageLocation)) {
            throw new IOException("Cannot store file outside of the permanent storage directory.");
        }

        // Ensure parent directory exists within the permanent storage
        Files.createDirectories(destinationFile.getParent());

        // Move the file from its source location to the permanent destination
        Files.move(sourceFile, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        log.info("Moved file from temporary location {} to permanent local storage at {}", sourceFile, destinationFile);

        // Construct the public URL
        String finalUrl = this.baseUrl + destinationKey;
        log.info("Generated public URL for content: {}", finalUrl);

        return finalUrl;
    }
}
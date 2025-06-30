package com.content_storage_service.service;

import com.content_storage_service.storage.StorageService;
import com.shortscreator.shared.dto.VideoUploadJobV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NoSuchElementException;

@Slf4j
@Service // This is a regular, non-profiled service. It should always be available.
@RequiredArgsConstructor
public class VideoUploadProcessorService {

    private final StorageService storageService;

    /**
     * Contains the core business logic for processing a video upload job.
     * This method is agnostic to the message source.
     *
     * @param job The deserialized job message.
     * @return The final URL of the uploaded video.
     * @throws NoSuchElementException If the source file does not exist.
     */
    public String processUploadJob(VideoUploadJobV1 job) {
        log.info("Processing video upload job for userId: {}", job.getUserId());
        Path sourcePath = Paths.get(job.getSourcePath());

        try {
            // 1. Check if the source file actually exists
            if (!Files.exists(sourcePath)) {
                log.error("Source file does not exist, cannot process job. File: {}. This is an unrecoverable error.", sourcePath);
                // We throw an exception that the listener can decide how to handle (e.g., DLQ).
                throw new NoSuchElementException("Source file not found: " + sourcePath);
            }

            // 2. Perform the upload using the storage service
            String finalUrl = storageService.store(sourcePath, job.getDestinationPath());
            log.info("Successfully stored video for userId: {}. Final URL: {}", job.getUserId(), finalUrl);

            // 3. Clean up the temporary file ONLY AFTER a successful upload
            //try {
            //    Files.delete(sourcePath);
            //    log.info("Successfully cleaned up temporary file: {}", sourcePath);
            //} catch (IOException e) {
            //    // This is not a fatal error for the job itself, so we just log a warning.
            //    log.warn("Upload was successful, but failed to clean up temporary file: {}", sourcePath, e);
            //}
            
            // 4. TODO: Send completion notification

            return finalUrl;
        } catch (IOException e) {
            // Failure during storageService.store() - this is a transient error.
            log.error("Storage operation failed for job: {}. Error: {}", job, e.getMessage());
            // Re-throw so the listener can trigger a retry.
            throw new RuntimeException("Failed during storage operation, will retry.", e);
        }
        // Note: The NoSuchElementException will propagate up to the listener.
    }
}
package com.content_generation_service.generation.service.storage;

import com.content_generation_service.generation.model.VideoMetadata;
import com.content_generation_service.generation.service.visual.VideoMetadataService;
import com.shortscreator.shared.dto.GeneratedVideoDetailsV1;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
@Profile("dev") // Only active in the 'dev' environment
public class LocalStorageService implements StorageService {

    private final Path localUploadPath;
    private final VideoMetadataService videoMetadataService;

    public LocalStorageService(@Value("${app.storage.local.upload-dir}") String uploadDir, VideoMetadataService videoMetadataService) throws IOException {
        this.localUploadPath = Paths.get(uploadDir);
        this.videoMetadataService = videoMetadataService;
        Files.createDirectories(this.localUploadPath);
    }

    @Override
    public GeneratedVideoDetailsV1 storeFinalVideo(Path localPath, String templateId, String contentId, String userId) {
        try {
            String fileName = templateId + "_" + contentId + "_" + UUID.randomUUID() + ".mp4";
            Path destinationPath = localUploadPath.resolve(fileName);
            // Get metadata BEFORE you clean up the local file
            VideoMetadata metadata = videoMetadataService.getMetadata(localPath);

            log.info("DEV MODE: Moving final video from [{}] to local storage [{}]", localPath, destinationPath);
            Files.move(localPath, destinationPath, StandardCopyOption.REPLACE_EXISTING);

            // Get video metadata
            double duration = metadata.duration();
            int width = metadata.width();
            int height = metadata.height();

            // Return a file URI for local access
            return new GeneratedVideoDetailsV1(destinationPath.toUri().toString(), destinationPath.toString(), duration, width, height);
        } catch (IOException e) {
            log.error("Failed to move local file for development storage", e);
            throw new RuntimeException("Local storage failed", e);
        }
    }

    @Override
    public void cleanupLocalFile(Path localPath) {
        // Not needed, because storeFinalVideo MOVES the file, which is a cleanup itself.
        log.debug("DEV MODE: Cleanup is handled by the move operation. No action taken.");
    }
}
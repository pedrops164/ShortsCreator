package com.content_generation_service.generation.service.storage;

import com.shortscreator.shared.dto.GeneratedVideoDetailsV1;
import java.nio.file.Path;

public interface StorageService {

    /**
     * Takes a locally generated video file and processes it for final storage.
     * In production, this uploads to S3. In development, it moves it to a local directory.
     *
     * @param localPath The path to the temporary generated video on the filesystem.
     * @param contentId The ID of the content for path construction.
     * @param userId The ID of the user.
     * @return Details about the final stored video.
     */
    GeneratedVideoDetailsV1 storeFinalVideo(Path localPath, String templateId, String contentId, String userId);

    /**
     * Cleans up the temporary local file.
     * Note: This might not be necessary if the storeFinalVideo implementation moves the file.
     */
    void cleanupLocalFile(Path localPath);
}
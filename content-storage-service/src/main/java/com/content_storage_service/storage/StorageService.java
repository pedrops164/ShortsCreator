package com.content_storage_service.storage;

import java.io.IOException;
import java.nio.file.Path;

public interface StorageService {
    /**
     * Stores a file and returns its publicly accessible URL.
     *
     * @param fileToStore The path to the local file that needs to be stored.
     * @param destinationPath The desired path or key in the destination storage (e.g., "videos/reddit/story1.mp4").
     * @return The final, accessible URL of the stored file.
     * @throws IOException If the storage operation fails.
     */
    String store(Path fileToStore, String destinationPath) throws IOException;
}
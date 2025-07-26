package com.content_generation_service.generation.service.visual;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Responsible for retrieving video assets, such as background videos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoAssetService {

    /**
     * Fetches the background video file based on its ID.
     * This implementation is a stub. A real implementation would fetch from
     * a cloud storage provider (e.g., S3) or a local asset directory.
     *
     * @param videoId The unique identifier for the background video.
     * @return The local file system path to the video.
     */
    public Path getBackgroundVideo(String videoId) throws IOException {
        log.info("Fetching background video for videoId: {}", videoId);

        String resourcePath = "assets/videos/" + videoId + ".mp4";
        URL resourceUrl = this.getClass().getClassLoader().getResource(resourcePath);

        if (resourceUrl == null) {
            throw new IOException("Background video not found in resources: " + resourcePath);
        }

        log.debug("Resolved background video URL: {}", resourceUrl);

        try {
            return Paths.get(resourceUrl.toURI());
        } catch (URISyntaxException e) {
            throw new IOException("Invalid URI for background video: " + resourceUrl, e);
        }
    }

    public Path getCharacterImage(String characterId) throws IOException {
        log.info("Fetching character image for characterId: {}", characterId);

        String resourcePath = "assets/characters/" + characterId + ".png";
        URL resourceUrl = this.getClass().getClassLoader().getResource(resourcePath);

        if (resourceUrl == null) {
            throw new IOException("Character image not found in resources: " + resourcePath);
        }

        log.debug("Resolved character image URL: {}", resourceUrl);

        try {
            return Paths.get(resourceUrl.toURI());
        } catch (URISyntaxException e) {
            throw new IOException("Invalid URI for character image: " + resourceUrl, e);
        }
    }
}

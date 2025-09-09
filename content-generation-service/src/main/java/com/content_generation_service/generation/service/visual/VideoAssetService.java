package com.content_generation_service.generation.service.visual;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.content_generation_service.config.AppProperties;
import com.content_generation_service.generation.service.assets.AssetProvider;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Responsible for retrieving video assets, such as background videos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoAssetService {

    private final AssetProvider assetProvider; // Inject the interface
    private final AppProperties appProperties; // Inject the config class

    /**
     * Fetches the background video file based on its ID.
     *
     * @param videoId The unique identifier for the background video.
     * @return The local file system path to the video.
     */
    public Path getBackgroundVideo(String videoId) throws IOException {
        log.info("Fetching background video for videoId: {}", videoId);
        String assetName = videoId + ".mp4";
        return assetProvider.getAssetPath(appProperties.getAssets().getVideos(), assetName);
    }

    /**
     * Fetches the character image file based on its ID.
     * @param characterId The unique identifier for the character image.
     * @return The local file system path to the character image.
     */
    public Path getCharacterImage(String characterId) throws IOException {
        log.info("Fetching character image for characterId: {}", characterId);
        String assetName = characterId + ".png";
        return assetProvider.getAssetPath(appProperties.getAssets().getCharacters(), assetName);
    }
}

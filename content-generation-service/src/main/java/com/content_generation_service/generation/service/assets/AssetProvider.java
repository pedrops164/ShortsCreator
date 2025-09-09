package com.content_generation_service.generation.service.assets;

import java.io.IOException;
import java.nio.file.Path;

public interface AssetProvider {
    /**
     * Provides a resolvable Path to an asset.
     *
     * @param subpath The subdirectory for the asset type (e.g., "assets/videos/").
     * @param assetName The name of the asset file (e.g., "minecraft_parkour.mp4").
     * @return A Path that can be used by services like FFmpeg or ImageIO.
     * @throws IOException if the asset cannot be found or accessed.
     */
    Path getAssetPath(String subpath, String assetName) throws IOException;
    
    Path getAssetDir(String subpath) throws IOException;
}
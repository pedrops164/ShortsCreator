package com.content_generation_service.generation.service.assets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileSystemAssetProvider implements AssetProvider {

    private final Path basePath;

    // Inject a generic base path from configuration
    public FileSystemAssetProvider(@Value("${app.assets.base-path}") String basePath) {
        this.basePath = Paths.get(basePath);
    }

    @Override
    public Path getAssetPath(String subpath, String assetName) throws IOException {
        Path assetPath = basePath.resolve(subpath).resolve(assetName);
        if (!Files.exists(assetPath)) {
            throw new IOException("Asset not found at path: " + assetPath);
        }
        return assetPath;
    }

    @Override
    public Path getAssetDir(String subpath) throws IOException {
        Path dirPath = basePath.resolve(subpath);
        if (!Files.isDirectory(dirPath)) {
            throw new IOException("Asset directory not found at path: " + dirPath);
        }
        return dirPath;
    }
}
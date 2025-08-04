package com.content_storage_service.controller;

import org.springframework.web.bind.annotation.*;

import com.content_storage_service.dto.AssetType;
import com.content_storage_service.model.Asset;
import com.content_storage_service.repository.AssetRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
@Slf4j
public class AssetController {

    private final AssetRepository assetRepository;

    /**
     * Retrieves assets by their type.
     *
     * @param type The AssetType to filter by.
     * @return A Flux<Asset> which will be serialized to the HTTP response.
     */
    @GetMapping
    public Flux<Asset> getAssetsByType(@RequestParam("type") AssetType type) {
        log.info("Reactively fetching assets of type: {}", type);
        return assetRepository.findByType(type)
                .doOnNext(asset -> log.trace("Streaming asset: {}", asset.getName()))
                .doOnError(error -> log.error("Error fetching assets for type {}", type, error));
    }
}
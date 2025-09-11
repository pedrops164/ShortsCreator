package com.content_storage_service.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import com.content_storage_service.enums.AssetType;
import com.content_storage_service.model.Asset;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface AssetRepository extends ReactiveMongoRepository<Asset, String> {

    /**
     * Finds all assets of a given type and returns them as a reactive stream.
     *
     * @param type The type of asset to find (e.g., VIDEO, MUSIC).
     * @return A Flux emitting the found assets.
     */
    Flux<Asset> findByType(AssetType type);
    
}

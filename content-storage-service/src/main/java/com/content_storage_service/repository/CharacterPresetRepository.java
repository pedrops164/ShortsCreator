package com.content_storage_service.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

import com.content_storage_service.model.CharacterPreset;

@Repository
public interface CharacterPresetRepository extends ReactiveMongoRepository<CharacterPreset, String> {
}
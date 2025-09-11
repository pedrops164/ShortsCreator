package com.content_storage_service.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.content_storage_service.enums.AssetType;

import lombok.Data;

@Document(collection = "assets")
@Data // Generates getters, setters, equals, hashCode, toString
public class Asset {
    @Id
    private String id;
    private String assetId;
    private AssetType type;
    private String name;
    private String category; // Nullable
    private String description; // Nullable
    private String thumbnailUrl; // Nullable
    private String sourceUrl; // Nullable
    private String displayName; // Nullable
}
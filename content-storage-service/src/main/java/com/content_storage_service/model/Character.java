package com.content_storage_service.model;

import lombok.Data;

@Data // Generates getters, setters, toString, etc.
public class Character {
    private String characterId; // e.g., "peter"
    private String name;
    private String avatarUrl;
}
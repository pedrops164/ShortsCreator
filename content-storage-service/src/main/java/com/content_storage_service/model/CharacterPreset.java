package com.content_storage_service.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import java.util.List;

@Data
@Document(collection = "character_presets")
public class CharacterPreset {
    @Id
    private String id; // The auto-generated MongoDB ObjectId
    private String presetId; // e.g., "peter_stewie"
    private String name;
    private List<Character> characters;
}
package com.content_storage_service.config;

import com.shortscreator.shared.enums.ContentType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class TemplateMappingConfig {

    /**
     * Defines a Bean for the static mapping from templateId to ContentType.
     * This map can be injected into any Spring component.
     */
    @Bean
    public Map<String, ContentType> templateToContentTypeMap() {
        Map<String, ContentType> map = new HashMap<>();
        map.put("reddit_story_v1", ContentType.REDDIT_STORY);
        map.put("character_explains_v1", ContentType.CHARACTER_EXPLAINS);
        //map.put("italian-brainrot", ContentType.ITALIAN_BRAINROT);
        return Collections.unmodifiableMap(map); // Make it unmodifiable to prevent runtime changes
    }
}
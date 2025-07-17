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
        map.put("italian-brainrot", ContentType.ITALIAN_BRAINROT);
        map.put("peter-explains-stuff", ContentType.PETER_EXPLAINS_STUFF);
        // Add other templateId -> ContentType mappings here
        return Collections.unmodifiableMap(map); // Make it unmodifiable to prevent runtime changes
    }
}
package com.content_generation_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shortscreator.shared.validation.TemplateValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ValidationConfig {

    @Bean
    public TemplateValidator templateValidator(ObjectMapper objectMapper) {
        // We use the Spring managed beans to manually construct our plain Java utility class.
        return new TemplateValidator(objectMapper);
    }
}
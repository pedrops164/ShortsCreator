package com.content_storage_service.config;

import com.content_storage_service.config.converters.DocumentToJsonNodeConverter;
import com.content_storage_service.config.converters.JsonNodeToDocumentConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import java.util.Arrays;

@Configuration
public class MongoConfig {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        // Register our two new converters with Spring Data
        return new MongoCustomConversions(Arrays.asList(
            new DocumentToJsonNodeConverter(),
            new JsonNodeToDocumentConverter()
        ));
    }
}
/* package com.content_storage_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfiguration implements WebMvcConfigurer {

    @Value("${app.storage.local.location}")
    private String storageLocation;

    @Value("${app.storage.local.url-path}")
    private String urlPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // This makes files in your storage directory accessible via HTTP
        // e.g., http://localhost:8080/local-videos/video.mp4 will map to temp/videos/video.mp4
        registry.addResourceHandler(urlPath + "/**")
                .addResourceLocations("file:" + storageLocation + "/");
    }
} */
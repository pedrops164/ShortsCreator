package com.content_storage_service.dto;

import com.content_storage_service.enums.GenerationType;

public record GeneratedContentResponse(
    GenerationType generationType,
    GeneratedContent content
) {}
package com.content_storage_service.dto;

public record OpenAiChatMessage(
    String role,
    String content
) {}
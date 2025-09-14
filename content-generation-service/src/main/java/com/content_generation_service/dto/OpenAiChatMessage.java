package com.content_generation_service.dto;

public record OpenAiChatMessage(
    String role,
    String content
) {}
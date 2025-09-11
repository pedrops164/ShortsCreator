package com.content_storage_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record OpenAiChatRequest(
    String model,
    List<OpenAiChatMessage> messages,
    Double temperature,
    @JsonProperty("max_tokens")
    Integer maxTokens
) {}
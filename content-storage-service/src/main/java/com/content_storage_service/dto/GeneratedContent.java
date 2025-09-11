package com.content_storage_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeneratedContent(
    String text,                      // For single-string responses
    List<String> comments,            // For a list of comments
    List<DialogueLine> dialogue       // For structured dialogue
) {}
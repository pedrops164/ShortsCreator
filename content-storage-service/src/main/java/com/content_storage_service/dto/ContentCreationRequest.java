package com.content_storage_service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.databind.JsonNode;

// DTO for creating a new content draft
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentCreationRequest {
    // Checking validity of templateId, contentType and templateParams are done in the service layer
    @NotNull(message = "Template ID cannot be null")
    private String templateId;
    @NotNull(message = "Template parameters cannot be null")
    private JsonNode templateParams;
}
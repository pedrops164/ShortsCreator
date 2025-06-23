package com.shortscreator.shared.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerationRequestV1 {
    private String contentId;
    private String userId;
    private String templateId;
    private JsonNode templateParams;
}
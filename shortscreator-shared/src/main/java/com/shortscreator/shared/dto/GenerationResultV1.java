package com.shortscreator.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.shortscreator.shared.enums.ContentStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerationResultV1 {
    private String contentId;
    private ContentStatus status; // e.g., "COMPLETED", "FAILED"
    private GeneratedVideoDetailsV1 generatedVideoDetails;
    private String errorMessage;
}
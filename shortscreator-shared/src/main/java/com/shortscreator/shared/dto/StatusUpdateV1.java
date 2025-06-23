package com.shortscreator.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.shortscreator.shared.enums.ContentStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatusUpdateV1 {
    private String contentId;
    private ContentStatus status; // e.g., "COMPLETED", "FAILED"
    private OutputAssetsV1 outputAssets;
    private String errorMessage;
}
package com.shortscreator.shared.dto;

import com.shortscreator.shared.enums.ContentStatus;

public record VideoStatusUpdateV1(
    String userId, 
    String contentId, 
    ContentStatus status,
    Double progressPercentage
) {
    // Compact constructor for validation (Java 16+)
    public VideoStatusUpdateV1 {
        if (status == ContentStatus.PROCESSING) {
            if (progressPercentage == null) {
                throw new IllegalArgumentException("PROCESSING status must have a progressPercentage.");
            }
            if (progressPercentage < 0.0 || progressPercentage > 100.0) {
                throw new IllegalArgumentException("Progress percentage must be between 0 and 100 for PROCESSING status.");
            }
        } else if (status == ContentStatus.COMPLETED) {
            // For completed, you can enforce 100% or allow null/ignore the field
            // Let's ensure it's 100% or null and we interpret null as 100%
            if (progressPercentage != null && progressPercentage != 100.0) {
                 throw new IllegalArgumentException("COMPLETED status must have 100% progress or null.");
            }
        } else if (status == ContentStatus.FAILED || status == ContentStatus.DRAFT) {
            if (progressPercentage != null) {
                throw new IllegalArgumentException("DRAFT or FAILED status must not have a progressPercentage.");
            }
        }
    }
}
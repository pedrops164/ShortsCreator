package com.shortscreator.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class OutputAssetsV1 {
    private String finalVideoUrl;
    private String finalVideoS3Key;
    private double durationSeconds;
}
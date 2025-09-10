package com.shortscreator.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedVideoDetailsV1 {
    private String s3Url;
    private String s3Key;
    private double durationSeconds;
    private int width;
    private int height;
}
package com.shortscreator.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A data transfer object representing a job to upload a video file.
 * This object is sent as a message to an SQS queue.
 *
 * @param sourcePath      The absolute path to the generated file on the shared filesystem (EFS).
 * @param destinationPath The target key for the file in the final storage bucket (S3).
 * @param contentId       The unique ID of the content being processed, for tracking.
 * @param userId          The ID of the user who initiated the request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoUploadJobV1 {
    private String sourcePath;
    private String destinationPath;
    private String userId;
}
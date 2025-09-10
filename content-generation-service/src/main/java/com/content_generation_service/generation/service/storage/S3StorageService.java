package com.content_generation_service.generation.service.storage;

import com.content_generation_service.generation.model.VideoMetadata;
import com.content_generation_service.generation.service.visual.VideoMetadataService;
import com.shortscreator.shared.dto.GeneratedVideoDetailsV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@Service
@Profile("prod") // Only active in the 'prod' environment
@RequiredArgsConstructor
public class S3StorageService implements StorageService {

    private final S3Client s3Client;
    private final VideoMetadataService videoMetadataService;
    
    @Value("${app.aws.s3.bucket-name}")
    private String bucketName;

    @Override
    public GeneratedVideoDetailsV1 storeFinalVideo(Path localPath, String templateId, String contentId, String userId) {
        String destinationKey = String.format("%s/%s/%s.mp4", templateId, contentId, UUID.randomUUID());
        // Get metadata BEFORE you clean up the local file
        VideoMetadata metadata = videoMetadataService.getMetadata(localPath);

        // Upload the file to S3
        log.info("Uploading file [{}] to S3 at s3://{}/{}", localPath.getFileName(), bucketName, destinationKey);
        String s3Url;
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(destinationKey)
                    .build();

            s3Client.putObject(request, localPath);
            s3Url = s3Client.utilities().getUrl(b -> b.bucket(bucketName).key(destinationKey)).toExternalForm();
            log.info("Successfully uploaded file to {}", s3Url);

        } catch (Exception e) {
            log.error("Failed to upload file {} to S3", localPath, e);
            throw new RuntimeException("S3 upload failed", e);
        }

        // Clean up the local file from EFS
        cleanupLocalFile(localPath);

        // Return the complete details
        return new GeneratedVideoDetailsV1(
            s3Url,
            destinationKey,
            metadata.duration(),
            metadata.width(),
            metadata.height()
        );
    }

    @Override
    public void cleanupLocalFile(Path localPath) {
        try {
            Files.deleteIfExists(localPath);
            log.info("Successfully cleaned up local file: {}", localPath);
        } catch (IOException e) {
            log.error("Failed to clean up local file: {}", localPath, e);
        }
    }
}
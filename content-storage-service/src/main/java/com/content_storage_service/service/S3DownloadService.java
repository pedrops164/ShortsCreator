package com.content_storage_service.service;

import com.content_storage_service.model.Content;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import java.time.Duration;

@Service
@Profile("prod") // Only active in the 'prod' environment
@RequiredArgsConstructor
public class S3DownloadService implements DownloadService {

    private final S3Presigner s3Presigner;
    
    @Value("${app.aws.s3.bucket-name}")
    private String bucketName;

    @Override
    public Mono<String> getDownloadUrl(Content content) {
        String key = content.getOutputAssets().getFinalVideoS3Key();
        String downloadFilename = "generated_video_" + content.getId() + ".mp4";
        
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            // This line tells S3 to add the download header to its response
            .responseContentDisposition("attachment; filename=\"" + downloadFilename + "\"")
            .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(5))
            .getObjectRequest(getObjectRequest)
            .build();
        
        // This is a synchronous call, which is fine for this quick operation
        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return Mono.just(presignedRequest.url().toString());
    }
}
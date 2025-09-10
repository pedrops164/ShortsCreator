package com.content_generation_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsConfig {

    @Bean
    @Profile("prod") // Only create this bean in the 'prod' profile
    public S3Client s3Client(@Value("${aws.region}") String awsRegion) {
        return S3Client.builder()
                // Specify the AWS Region for the client
                .region(Region.of(awsRegion))
                
                // Use the recommended DefaultCredentialsProvider.
                // This automatically finds credentials from the local ~/.aws/credentials file,
                // environment variables, or the ECS Task Role in production.
                .credentialsProvider(DefaultCredentialsProvider.create())
                
                // Specify the HTTP client.
                .httpClient(UrlConnectionHttpClient.builder().build())
                
                .build();
    }
}
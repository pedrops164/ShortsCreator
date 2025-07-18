package com.content_generation_service.generation.orchestrator;

import com.content_generation_service.generation.service.reddit.audio.RedditTextToSpeechService;
import com.content_generation_service.generation.service.reddit.visual.RedditImageService;
import com.content_generation_service.generation.service.visual.SubtitleService;
import com.content_generation_service.generation.service.visual.VideoAssetService;
import com.content_generation_service.generation.service.visual.VideoCompositionBuilder;
import com.content_generation_service.generation.model.RedditNarration;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import com.shortscreator.shared.dto.VideoUploadJobV1;


@Slf4j
@Service
@RequiredArgsConstructor
public class RedditStoryOrchestrator {

    public static final String REDDIT_STORY_TEMPLATE_ID = "reddit_story_v1";

    private final RedditTextToSpeechService textToSpeechService;
    private final VideoAssetService videoAssetService;
    private final SubtitleService subtitleService;
    private final RedditImageService redditImageService;

    // Use a clear property for the shared temporary path
    @Value("${app.storage.shared-temp.base-path}")
    private String sharedTempBasePath;
    
    // This is the main business logic flow
    public VideoUploadJobV1 generate(JsonNode params, String contentId, String userId) {
        log.info("Starting Reddit Story generation...");

        // Ensure the shared directory exists before we start
        Path sharedOutputPath = Paths.get(sharedTempBasePath);
        try {
            Files.createDirectories(sharedOutputPath);
        } catch (IOException e) {
            log.error("Cannot create shared temporary directory at: {}", sharedOutputPath, e);
            throw new RuntimeException("Failed to initialize shared storage", e);
        }
        
        Path finalVideoPath;
        try {
            // 1. Get narration from TTS API
            RedditNarration narration = textToSpeechService.generateNarration(params.get("postTitle").asText(), params.get("postDescription").asText(), params.get("comments"), params.get("voiceSelection").asText());
            
            // 2. Get background video
            Path backgroundVideo = videoAssetService.getBackgroundVideo(params.get("backgroundVideoId").asText());

            // 3. Create image for post title
            Path titleImage = redditImageService.createRedditPostImage(params);

            // 4. Generate subtitles from the audio timings
            Path subtitleFile = subtitleService.createAssFile(narration.getWordTimings(), params);

            // 5. Combine everything into a final video composition
            VideoCompositionBuilder compositionBuilder = new VideoCompositionBuilder();
            finalVideoPath = compositionBuilder
                .withBackground(backgroundVideo, 1080, 1920) // Assuming 9:16 aspect ratio
                .withNarration(narration.getAudioFilePath())
                .withOverlay(titleImage, narration.getTitleDurationSeconds(), false)
                .withSubtitles(subtitleFile)
                .withOutputDuration(narration.getDurationSeconds())
                .buildAndExecute(sharedOutputPath);
        } catch (Exception e) {
            log.error("Video composition failed for contentId: {}", contentId, e);
            throw new RuntimeException("Failed to compose final video", e);
        }

        // --- Step 6: Create and send the generation result message ---
        String destinationPath = String.format("reddit-stories/%s/%s.mp4", contentId, UUID.randomUUID());

        VideoUploadJobV1 job = new VideoUploadJobV1(
            finalVideoPath.toAbsolutePath().toString(),
            destinationPath,
            userId
        );
        return job;
    }
}
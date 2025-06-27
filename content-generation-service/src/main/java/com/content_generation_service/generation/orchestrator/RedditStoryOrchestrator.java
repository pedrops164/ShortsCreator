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

import java.nio.file.Path;

import org.springframework.stereotype.Service;
import com.shortscreator.shared.dto.OutputAssetsV1;


@Slf4j
@Service
@RequiredArgsConstructor
public class RedditStoryOrchestrator {

    public static final String REDDIT_STORY_TEMPLATE_ID = "reddit_story_v1";
    private final RedditTextToSpeechService textToSpeechService;
    private final VideoAssetService videoAssetService;
    private final SubtitleService subtitleService;
    private final RedditImageService redditImageService;
    
    // This is the main business logic flow
    public OutputAssetsV1 generate(JsonNode params) {
        log.info("Starting Reddit Story generation...");

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
            Path finalVideo = compositionBuilder
                .withBackground(backgroundVideo, 1080, 1920) // Assuming 9:16 aspect ratio
                .withNarration(narration.getAudioFilePath())
                .withOverlay(titleImage, narration.getTitleDurationSeconds(), false)
                .withSubtitles(subtitleFile)
                .withOutputDuration(narration.getDurationSeconds())
                .buildAndExecute();

            // 6. Upload final video to cloud storage (e.g., S3) and get URL
            String finalVideoUrl = "https://your-cloud-storage.com/" + finalVideo.getFileName();
            log.info("Reddit Story generation complete. Final video at {}", finalVideoUrl);

            return new OutputAssetsV1(finalVideoUrl, 60); // Return the final result
        } catch (Exception e) {
            log.error("Error during Reddit Story generation", e);
            throw new RuntimeException("Failed to generate Reddit Story", e);
        }
    }
}
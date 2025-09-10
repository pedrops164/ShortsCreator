package com.content_generation_service.generation.orchestrator;

import com.content_generation_service.generation.service.assets.AssetProvider;
import com.content_generation_service.generation.service.audio.AudioService;
import com.content_generation_service.generation.service.audio.TextToSpeechProvider;
import com.content_generation_service.generation.service.audio.TextToSpeechService.ParsedVoiceId;
import com.content_generation_service.generation.service.reddit.visual.RedditImageService;
import com.content_generation_service.generation.service.storage.StorageService;
import com.content_generation_service.generation.service.visual.ProgressListener;
import com.content_generation_service.generation.service.visual.SubtitleService;
import com.content_generation_service.generation.service.visual.VideoAssetService;
import com.content_generation_service.generation.service.visual.VideoCompositionBuilder;
import com.content_generation_service.messaging.VideoStatusUpdateDispatcher;
import com.content_generation_service.config.AppProperties;
import com.content_generation_service.generation.model.NarrationSegment;
import com.content_generation_service.generation.model.RedditNarration;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;

import com.shortscreator.shared.dto.GeneratedVideoDetailsV1;

import com.content_generation_service.generation.service.audio.TextToSpeechService;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedditStoryOrchestrator {

    public static final String REDDIT_STORY_TEMPLATE_ID = "reddit_story_v1";

    private final TextToSpeechService textToSpeechService;
    private final VideoAssetService videoAssetService;
    private final SubtitleService subtitleService;
    private final RedditImageService redditImageService;
    private final VideoStatusUpdateDispatcher videoStatusUpdateDispatcher;
    private final AudioService audioService;

    private final ObjectProvider<VideoCompositionBuilder> videoCompositionBuilderProvider;
    private final AssetProvider assetProvider;
    private final AppProperties appProperties;

    // Service responsible for uploading to S3 and cleaning up local files
    private final StorageService storageService;

    // Use a clear property for the shared temporary path
    @Value("${app.storage.shared-temp.base-path}")
    private String sharedTempBasePath;
    
    // This is the main business logic flow
    public GeneratedVideoDetailsV1 generate(JsonNode params, String contentId, String userId) {
        log.info("Starting Reddit Story generation...");

        // Create a scoped listener for this specific content generation
        ProgressListener scopedProgressListener = videoStatusUpdateDispatcher.forContent(userId, contentId);

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
            // Get narration from TTS API
            RedditNarration narration = generateNarration(params.get("postTitle").asText(), params.get("postDescription").asText(), params.get("comments"), params.get("voiceSelection").asText());
            
            // Get background video
            Path backgroundVideo = videoAssetService.getBackgroundVideo(params.get("backgroundVideoId").asText());

            // Create image for post title
            Path titleImage = redditImageService.createRedditPostImage(params);

            // Generate subtitles from the audio timings
            JsonNode subtitles = params.get("subtitles");
            String font = subtitles.get("font").asText("Arial");
            //String font = "Montserrat ExtraBold"; // test
            String color = subtitles.get("color").asText("#FFFFFF");
            String position = subtitles.get("position").asText("bottom");
            Path subtitleFile = subtitleService.createAssFile(narration.getWordTimings(), font, color, position);
            // Use the AssetProvider to get the path to the FONTS directory
            Path fontDirPath = assetProvider.getAssetDir(appProperties.getAssets().getFonts());

            // Get dimensions from AppProperties
            int videoWidth = appProperties.getVideo().getWidth();
            int videoHeight = appProperties.getVideo().getHeight();
            // Combine everything into a final video composition
            VideoCompositionBuilder compositionBuilder = videoCompositionBuilderProvider.getObject();
            finalVideoPath = compositionBuilder
                .withDimensions(videoWidth, videoHeight)
                .withBackground(backgroundVideo) // Assuming 9:16 aspect ratio
                .withNarration(narration.getAudioFilePath())
                .withCenteredOverlay(titleImage, 0, narration.getTitleDurationSeconds(), false)
                .withSubtitles(fontDirPath, subtitleFile)
                .withOutputDuration(narration.getDurationSeconds())
                .withProgressListener(scopedProgressListener) // Pass the scoped listener
                .buildAndExecute(sharedOutputPath);
        } catch (Exception e) {
            log.error("Video composition failed for contentId: {}", contentId, e);
            scopedProgressListener.onError(); // Notify the listener of failure
            throw new RuntimeException("Failed to compose final video", e);
        }

        GeneratedVideoDetailsV1 videoDetails = storageService.storeFinalVideo(finalVideoPath, REDDIT_STORY_TEMPLATE_ID, contentId, userId);
        scopedProgressListener.onComplete(); // Notify the listener of success
        return videoDetails;
    }


    /**
     * Generates separate narration for title, description, and comments, then combines them.
     * This method blocks until all API calls and processing are complete.
     */
    public RedditNarration generateNarration(String title, String description, JsonNode comments, String globalVoiceId) {
        ParsedVoiceId parsedId = TextToSpeechService.parseGlobalVoiceId(globalVoiceId);
        TextToSpeechProvider ttsProvider = textToSpeechService.getProvider(parsedId.providerId());

        if (ttsProvider == null) {
            throw new IllegalArgumentException("Unsupported TTS provider: " + parsedId.providerId());
        }

        String voice = parsedId.voiceId();
        log.info("Generating Reddit narration using provider: {}, voice: {}", ttsProvider.getProviderId(), voice);

        Mono<NarrationSegment> titleMono = ttsProvider.generate(title, voice, false);
        Mono<NarrationSegment> descriptionMono = ttsProvider.generate(description, voice, true);
        
        List<Mono<NarrationSegment>> commentMonos = new ArrayList<>();
        if (comments.isArray()) {
            for (JsonNode commentNode : comments) {
                String commentText = commentNode.get("text").asText();
                commentMonos.add(ttsProvider.generate(commentText, voice, true));
            }
        }

        // Execute all TTS calls in parallel and process the results
        return Mono.zip(titleMono, descriptionMono, Flux.concat(commentMonos).collectList())
            .flatMap(tuple -> {
                NarrationSegment titleNarration = tuple.getT1();
                NarrationSegment descriptionNarration = tuple.getT2();
                List<NarrationSegment> commentNarrations = tuple.getT3();

                // Build the full list of segments for combination
                List<NarrationSegment> allSegments = new ArrayList<>();
                allSegments.add(titleNarration);
                allSegments.add(descriptionNarration);
                allSegments.addAll(commentNarrations);

                // Use the inherited method to combine audio and adjust timestamps
                return audioService.combineAudioTracks(allSegments)
                    .map(combinedSegment -> new RedditNarration(
                        combinedSegment.getAudioFilePath(),
                        combinedSegment.getDurationSeconds(),
                        combinedSegment.getWordTimings(),
                        titleNarration.getDurationSeconds() // Keep track of title duration specifically
                    ));
            }).block(); // Block until the entire process is complete
    }
}
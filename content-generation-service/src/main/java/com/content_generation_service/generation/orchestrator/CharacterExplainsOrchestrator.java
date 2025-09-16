package com.content_generation_service.generation.orchestrator;

import com.content_generation_service.config.AppProperties;
import com.content_generation_service.generation.model.*;
import com.content_generation_service.generation.service.CharacterDialogueEnrichmentService;
import com.content_generation_service.generation.service.assets.AssetProvider;
import com.content_generation_service.generation.service.audio.AudioService;
import com.content_generation_service.generation.service.google.GoogleImageSearchService;
import com.content_generation_service.generation.service.speechify.audio.SpeechifyVoiceCloningProvider;
import com.content_generation_service.generation.service.storage.StorageService;
import com.content_generation_service.generation.service.visual.*;
import com.content_generation_service.messaging.VideoStatusUpdateDispatcher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.shortscreator.shared.dto.GeneratedVideoDetailsV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.awt.Dimension;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class CharacterExplainsOrchestrator {

    public static final String CHARACTER_EXPLAINS_TEMPLATE_ID = "character_explains_v1";

    // --- Core Services ---
    private final SpeechifyVoiceCloningProvider textToSpeechProvider;
    private final CharacterDialogueEnrichmentService dialogueEnrichmentService;
    private final GoogleImageSearchService googleImageSearchService;
    private final AudioService audioService;
    private final StorageService storageService;

    // --- Visual & Asset Services ---
    private final VideoAssetService videoAssetService;
    private final SubtitleService subtitleService;
    private final ImageUtilitiesService imageUtilitiesService;
    private final AssetProvider assetProvider;
    private final ObjectProvider<VideoCompositionBuilder> videoCompositionBuilderProvider;

    // --- Messaging & Config ---
    private final VideoStatusUpdateDispatcher videoStatusUpdateDispatcher;
    private final AppProperties appProperties;

    @Value("${app.storage.shared-temp.base-path}")
    private String sharedTempBasePath;

    public GeneratedVideoDetailsV1 generate(JsonNode params, String contentId, String userId) {
        log.info("Starting Character Explains generation for contentId: {}", contentId);
        ProgressListener scopedProgressListener = videoStatusUpdateDispatcher.forContent(userId, contentId);

        // A thread-safe list to collect all temporary files for cleanup
        List<Path> tempFiles = new CopyOnWriteArrayList<>();

        try {
            // Check if we need to generate images
            boolean generateImages = params.path("generateImages").asBoolean(false);
            JsonNode dialogue = params.get("dialogue");
            if (generateImages && (dialogue == null || !dialogue.isArray() || ((ArrayNode) dialogue).isEmpty())) {
                throw new IllegalArgumentException("Dialogue must be a non-empty array when generateImages is true.");
            }
            if (generateImages) {
                // Enrich Dialogue with Search Queries using LLM
                dialogue = dialogueEnrichmentService.enrichDialogueWithSearchQueries(params.get("dialogue")).block();
                if (dialogue == null) throw new RuntimeException("Failed to get enriched dialogue from LLM.");
                log.debug("Enriched Dialogue: {}", dialogue);
            }

            // Generate Audio & Download Images Concurrently
            List<DialogueLineResult> dialogueResults = generateMediaAssets(dialogue, generateImages).block();
            if (dialogueResults == null) throw new RuntimeException("Failed to generate media assets.");
            dialogueResults.forEach(res -> {
                log.debug("Dialogue Line - Narration: {}, Images: {}", res.narrationSegment(), res.imagePaths());
            });

            // Collect temp files from results
            dialogueResults.forEach(res -> {
                tempFiles.add(res.narrationSegment().getAudioFilePath());
                tempFiles.addAll(res.imagePaths());
            });

            // Process Timings and Combine Audio
            MediaAssets mediaAssets = processAndCombineAssets(dialogueResults, dialogue);
            tempFiles.add(mediaAssets.narration().getAudioFilePath()); // Add combined audio to cleanup list

            // Prepare Video Composition Assets
            Path backgroundVideo = videoAssetService.getBackgroundVideo(params.get("backgroundVideoId").asText());
            Path subtitleFile = createSubtitleFile(mediaAssets.narration(), params.get("subtitles"));
            tempFiles.add(subtitleFile);

            Map<String, Path> characterImageMap = getCharacterImages(params.get("characterPresetId").asText());

            // Build the Final Video
            VideoCompositionBuilder builder = buildVideoComposition(
                backgroundVideo,
                subtitleFile,
                characterImageMap,
                mediaAssets,
                scopedProgressListener
            );
            Path finalVideoPath = builder.buildAndExecute(Paths.get(sharedTempBasePath));

            // Store and Return
            GeneratedVideoDetailsV1 videoDetails = storageService.storeFinalVideo(finalVideoPath, CHARACTER_EXPLAINS_TEMPLATE_ID, contentId, userId);
            scopedProgressListener.onComplete();
            return videoDetails;
        } catch (Exception e) {
            log.error("Video composition failed for contentId: {}", contentId, e);
            scopedProgressListener.onError();
            throw new RuntimeException("Failed to compose final video", e);
        } finally {
            cleanupTempFiles(tempFiles);
        }
    }

    private Mono<List<DialogueLineResult>> generateMediaAssets(JsonNode dialogue, boolean generateImages) {
        return Flux.fromIterable(dialogue)
            .flatMapSequential(line -> {
                String characterId = line.get("characterId").asText();
                String text = line.get("text").asText();
                List<String> queries = generateImages ?
                    StreamSupport.stream(line.get("query_list").spliterator(), false)
                        .map(JsonNode::asText)
                        .toList()
                    : List.of();

                Mono<NarrationSegment> audioMono = textToSpeechProvider.generate(text, characterId, true).cache();

                Mono<List<Path>> imagesMono = Flux.fromIterable(queries)
                    .flatMapSequential(query -> googleImageSearchService.downloadImageForQuery(query)
                        // this makes the process fault tolerant to individual image download failures
                        .timeout(Duration.ofSeconds(10))
                        .onErrorResume(e -> {
                            log.warn("Could not download image for query '{}'. Skipping it. Reason: {}", query, e.getMessage());
                            return Mono.empty(); // On error, return an empty Mono to skip this element
                        })
                    )
                    .collectList();
                
                // Execute audio generation and image downloads in parallel for this line
                return Mono.zip(audioMono, imagesMono, DialogueLineResult::new);
            })
            .collectList();
    }
    
    private MediaAssets processAndCombineAssets(List<DialogueLineResult> results, JsonNode dialogue) {
        List<NarrationSegment> audioSegments = results.stream().map(DialogueLineResult::narrationSegment).toList();
        
        // This is a blocking call, but it happens after all reactive work is done.
        CharacterNarration combinedNarration = combineNarration(audioSegments, dialogue).block();
        if (combinedNarration == null) throw new RuntimeException("Failed to combine audio tracks.");

        List<ImageOverlaySegment> imageOverlays = new ArrayList<>();
        double currentTime = 0.0;

        for (DialogueLineResult result : results) {
            double lineDuration = result.narrationSegment().getDurationSeconds();
            List<Path> imagePaths = result.imagePaths();
            
            if (!imagePaths.isEmpty()) {
                // This is the full time slot allocated for each image within the line's audio.
                double originalDurationPerImage = lineDuration / imagePaths.size();

                for (Path imagePath : imagePaths) {
                    // Calculate the padding and the new, shorter visible duration.
                    double paddingDuration = originalDurationPerImage / 5.0;
                    double newVisibleDuration = originalDurationPerImage * 3.0 / 5.0;
                
                    // The image will appear after the initial padding.
                    double newStartTime = currentTime + paddingDuration;
                    
                    // Create the segment with the new, adjusted timing.
                    TimeRange timeRange = new TimeRange(newStartTime, newStartTime + newVisibleDuration);
                    imageOverlays.add(new ImageOverlaySegment(
                        imagePath,
                        newVisibleDuration, // Use the shorter duration
                        timeRange,
                        ImagePosition.TOP_HALF
                    ));

                    // Advance the master timeline by the *original* full duration.
                    // This ensures the next image slot starts at the correct time relative to the audio.
                    currentTime += originalDurationPerImage;
                }
            } else {
                 currentTime += lineDuration;
            }
        }
        return new MediaAssets(combinedNarration, imageOverlays);
    }

    private Mono<CharacterNarration> combineNarration(List<NarrationSegment> segments, JsonNode dialogue) {
        List<DialogueLineInfo> dialogueTimings = new ArrayList<>();
        double currentOffset = 0.0;
        for (int i = 0; i < segments.size(); i++) {
            NarrationSegment segment = segments.get(i);
            JsonNode line = dialogue.get(i);
            dialogueTimings.add(new DialogueLineInfo(
                line.path("characterId").asText(),
                currentOffset,
                segment.getDurationSeconds()
            ));
            currentOffset += segment.getDurationSeconds();
        }

        return audioService.combineAudioTracks(segments)
            .map(combinedSegment -> new CharacterNarration(
                combinedSegment.getAudioFilePath(),
                combinedSegment.getDurationSeconds(),
                combinedSegment.getWordTimings(),
                dialogueTimings
            ));
    }

    private Path createSubtitleFile(CharacterNarration narration, JsonNode subtitleParams) throws IOException {
        String font = subtitleParams.get("font").asText("Arial");
        String color = subtitleParams.get("color").asText("#FFFFFF");
        String position = subtitleParams.get("position").asText("bottom");
        return subtitleService.createAssFile(narration.getWordTimings(), font, color, position);
    }
    
    private Map<String, Path> getCharacterImages(String presetId) {
        return Arrays.stream(presetId.split("_"))
            .collect(Collectors.toMap(
                characterId -> characterId,
                characterId -> {
                    try {
                        return videoAssetService.getCharacterImage(characterId);
                    } catch (IOException e) {
                        throw new RuntimeException("Error fetching character image for " + characterId, e);
                    }
                }
            ));
    }

    private VideoCompositionBuilder buildVideoComposition(Path backgroundVideo, Path subtitleFile, Map<String, Path> characterImageMap, MediaAssets assets, ProgressListener listener) throws IOException {
        VideoCompositionBuilder builder = videoCompositionBuilderProvider.getObject()
            .withDimensions(appProperties.getVideo().getWidth(), appProperties.getVideo().getHeight())
            .withBackground(backgroundVideo)
            .withNarration(assets.narration().getAudioFilePath())
            .withProgressListener(listener);

        // Add character pop-ups
        addCharacterOverlays(builder, assets.narration(), characterImageMap);

        // Add searched image overlays
        for (ImageOverlaySegment overlay : assets.imageOverlays()) {
            builder.withImageOverlay(
                overlay.getImagePath(),
                overlay.getPosition(), // New method in builder to handle enum
                overlay.getTimeRange().getStartTimeSeconds(),
                overlay.getDurationSeconds()
            );
        }

        builder.withTextWatermark();

        // Add subtitles last so they are on top
        Path fontDirPath = assetProvider.getAssetDir(appProperties.getAssets().getFonts());
        builder.withSubtitles(fontDirPath, subtitleFile);
        
        return builder;
    }

    private void addCharacterOverlays(VideoCompositionBuilder builder, CharacterNarration narration, Map<String, Path> characterImageMap) throws IOException {
        List<String> characterOrder = narration.getDialogueTimings().stream()
                .map(DialogueLineInfo::getCharacterId)
                .distinct()
                .toList();

        int horizontalMargin = 50;
        for (DialogueLineInfo line : narration.getDialogueTimings()) {
            Path characterImage = characterImageMap.get(line.getCharacterId());
            Dimension imgDimensions = imageUtilitiesService.getImageDimensions(characterImage);
            
            int characterIndex = characterOrder.indexOf(line.getCharacterId());
            boolean isLeft = (characterIndex == 0);
            
            int y = builder.getHeight() - imgDimensions.height;
            int x = isLeft ? horizontalMargin : builder.getWidth() - imgDimensions.width - horizontalMargin;

            builder.withImageOverlay(characterImage, x, y, line.getStartTime(), line.getDuration(), false);
        }
    }

    private void cleanupTempFiles(List<Path> tempFiles) {
        log.info("Cleaning up {} temporary files...", tempFiles.size());
        for (Path file : tempFiles) {
            try {
                if (file != null) Files.deleteIfExists(file);
            } catch (IOException e) {
                log.warn("Failed to delete temporary file: {}", file, e);
            }
        }
    }
    
    // Helper records for cleaner data flow
    private record DialogueLineResult(NarrationSegment narrationSegment, List<Path> imagePaths) {}
    private record MediaAssets(CharacterNarration narration, List<ImageOverlaySegment> imageOverlays) {}
}
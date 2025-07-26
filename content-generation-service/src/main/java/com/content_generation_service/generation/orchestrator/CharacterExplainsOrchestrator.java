package com.content_generation_service.generation.orchestrator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.content_generation_service.generation.model.CharacterNarration;
import com.content_generation_service.generation.model.DialogueLineInfo;
import com.content_generation_service.generation.model.NarrationSegment;
import com.content_generation_service.generation.service.audio.AudioService;
import com.content_generation_service.generation.service.speechify.audio.SpeechifyVoiceCloningProvider;
import com.content_generation_service.generation.service.visual.ProgressListener;
import com.content_generation_service.generation.service.visual.SubtitleService;
import com.content_generation_service.generation.service.visual.VideoAssetService;
import com.content_generation_service.generation.service.visual.VideoCompositionBuilder;
import com.content_generation_service.messaging.VideoStatusUpdateDispatcher;
import com.fasterxml.jackson.databind.JsonNode;
import com.shortscreator.shared.dto.VideoUploadJobV1;

@Slf4j
@Service
@RequiredArgsConstructor
public class CharacterExplainsOrchestrator {


    public static final String CHARACTER_EXPLAINS_TEMPLATE_ID = "character_explains_v1";

    private final SpeechifyVoiceCloningProvider textToSpeechProvider;
    private final VideoAssetService videoAssetService;
    private final SubtitleService subtitleService;
    private final VideoStatusUpdateDispatcher videoStatusUpdateDispatcher;
    private final AudioService audioService;

    // Use a clear property for the shared temporary path
    @Value("${app.storage.shared-temp.base-path}")
    private String sharedTempBasePath;

    public VideoUploadJobV1 generate(JsonNode params, String contentId, String userId) {
        log.info("Starting Character Explains generation...");

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
            // Get character narration from the voice clone tts provider
            CharacterNarration narration = generateNarration(params.get("dialogue"));

            // Get background video
            Path backgroundVideo = videoAssetService.getBackgroundVideo(params.get("backgroundVideoId").asText());

            // Parse preset and get images for the characters involved
            /* String presetId = params.get("characterPresetId").asText(); // e.g., "peter_stewie"
            Map<String, Path> characterImageMap = Arrays.stream(presetId.split("_"))
                    .collect(Collectors.toMap(
                            characterId -> characterId,
                            characterId -> {
                                try {
                                    return videoAssetService.getCharacterImage(characterId); // Assuming new method in VideoAssetService
                                } catch (IOException e) {
                                    log.error("Failed to get character image for characterId: {}", characterId, e);
                                    throw new RuntimeException("Error fetching character image", e);
                                }
                            }
                    )); */

            // Extract styling parameters from the JsonNode
            JsonNode subtitles = params.get("subtitles");
            String font = subtitles.get("font").asText("Arial");
            String color = subtitles.get("color").asText("#FFFFFF");
            String position = subtitles.get("position").asText("bottom");
            // Generate subtitles from the audio timings
            Path subtitleFile = subtitleService.createAssFile(narration.getWordTimings(), font, color, position);

            // Combine everything into a final video composition
            VideoCompositionBuilder compositionBuilder = new VideoCompositionBuilder();
            finalVideoPath = compositionBuilder
                .withBackground(backgroundVideo, 1080, 1920)
                .withNarration(narration.getAudioFilePath())
                .withSubtitles(subtitleFile)
                .withProgressListener(scopedProgressListener)
                .buildAndExecute(sharedOutputPath);
        } catch (Exception e) {
            log.error("Video composition failed for contentId: {}", contentId, e);
            scopedProgressListener.onError(); // Notify the listener of failure
            throw new RuntimeException("Failed to compose final video", e);
        }

        // Create and return the video upload job
        String destinationPath = String.format("character-explains/%s/%s.mp4", contentId, UUID.randomUUID());
        VideoUploadJobV1 job = new VideoUploadJobV1(
            finalVideoPath.toAbsolutePath().toString(),
            destinationPath,
            userId
        );
        scopedProgressListener.onComplete();
        return job;
    }

    /**
     * Generates a single narration track from a dialogue script, including detailed timing info.
     * Each line of the dialogue is converted to speech and audios are concatenated.
     *
     * @param dialogue The JSON array containing the dialogue objects.
     * @return A CharacterNarration object containing the final audio and timing data.
     */
    public CharacterNarration generateNarration(JsonNode dialogue) {
        log.info("Generating narration for 'Character Explains' dialogue...");
        if (!dialogue.isArray()) {
            throw new IllegalArgumentException("Dialogue parameter must be a JSON array.");
        }

        List<Mono<NarrationSegment>> narrationMonos = new ArrayList<>();
        for (JsonNode line : dialogue) {
            // get id of the character and text to be spoken
            if (!line.has("characterId") || !line.has("text")) {
                throw new IllegalArgumentException("Each dialogue line must contain 'characterId' and 'text' fields.");
            }
            String characterId = line.path("characterId").asText();
            // we know text has minLength of 1, so no need to check length
            String text = line.path("text").asText();
            
            // generate the narration segment using the TTS provider, and add to list of narrations
            narrationMonos.add(textToSpeechProvider.generate(text, characterId, true));
        }

        return Flux.concat(narrationMonos)
            .collectList()
            .flatMap(segments -> {
                // First, calculate the timing info for each dialogue line
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

                // Now, combine the audio tracks and create the final CharacterNarration object
                return audioService.combineAudioTracks(segments)
                    .map(combinedSegment -> new CharacterNarration(
                        combinedSegment.getAudioFilePath(),
                        combinedSegment.getDurationSeconds(),
                        combinedSegment.getWordTimings(),
                        dialogueTimings
                    ));
            }).block();
    }
}

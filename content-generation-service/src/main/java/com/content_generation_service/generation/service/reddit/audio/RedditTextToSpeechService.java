package com.content_generation_service.generation.service.reddit.audio;

import com.content_generation_service.generation.model.NarrationSegment;
import com.content_generation_service.generation.model.RedditNarration;
import com.content_generation_service.generation.model.WordTiming;
import com.content_generation_service.generation.service.audio.TextToSpeechProvider;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * High-level service for handling text-to-speech generation. It orchestrates
 * calls to a configured TextToSpeechProvider for different parts of the content.
 */
@Slf4j
@Service
//@RequiredArgsConstructor
public class RedditTextToSpeechService {

    // A map to hold all available TTS providers, keyed by their unique ID (e.g., "openai").
    private final Map<String, TextToSpeechProvider> providerMap;

    /**
     * Constructs the service and builds a map of all available TTS providers.
     * Spring will automatically inject a list of all beans that implement TextToSpeechProvider.
     *
     * @param providers The list of all available TextToSpeechProvider beans.
     */
    public RedditTextToSpeechService(List<TextToSpeechProvider> providers) {
        // Create a map from the list for efficient lookups.
        // The key is the provider's ID (e.g., "openai"), and the value is the provider instance.
        // This will fail on startup if two providers have the same ID, which is a good safety check.
        this.providerMap = providers.stream()
                .collect(Collectors.toMap(TextToSpeechProvider::getProviderId, Function.identity()));
        log.info("Initialized RedditTextToSpeechService with available providers: {}", providerMap.keySet());
    }

    /**
     * A small, immutable data carrier for the parsed voice ID.
     */
    private record ParsedVoiceId(String providerId, String voiceId) {}

    /**
     * Parses the global voice ID string (e.g., "openai_echo") into its components.
     *
     * @param globalVoiceId The combined ID string.
     * @return A ParsedVoiceId containing the provider and voice IDs.
     * @throws IllegalArgumentException if the format is invalid.
     */
    private ParsedVoiceId parseGlobalVoiceId(String globalVoiceId) {
        if (globalVoiceId == null || !globalVoiceId.contains("_")) {
            throw new IllegalArgumentException("Invalid globalVoiceId format. Expected 'provider_voiceid', but got: " + globalVoiceId);
        }
        String[] parts = globalVoiceId.split("_", 2);
        if (parts.length < 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new IllegalArgumentException("Invalid globalVoiceId format. Both provider and voiceid must be non-empty.");
        }
        return new ParsedVoiceId(parts[0], parts[1]);
    }

    /**
     * Generates separate narration for title, description, and comments, then combines them.
     * This method is now synchronous and blocks until all API calls are complete.
     * A fully reactive implementation would return a Mono<RedditNarration>.
     */
    public RedditNarration generateNarration(String title, String description, JsonNode comments, String globalVoiceId) {
        // Parse the global voice ID to get the provider and specific voice.
        ParsedVoiceId parsedId = parseGlobalVoiceId(globalVoiceId);
        TextToSpeechProvider ttsProvider = providerMap.get(parsedId.providerId());

        if (ttsProvider == null) {
            log.error("No TTS provider found for ID: '{}'. Available providers: {}", parsedId.providerId(), providerMap.keySet());
            throw new IllegalArgumentException("Unsupported TTS provider: " + parsedId.providerId());
        }

        String voice = parsedId.voiceId(); // The actual voice for the provider (e.g., "echo")
        log.info("Generating full narration using TTS provider: {}, voice: {}", ttsProvider.getProviderId(), voice);

        // Generate each segment of audio sequentially.
        Mono<NarrationSegment> titleMono = ttsProvider.generate(title, voice, false);
        Mono<NarrationSegment> descriptionMono = ttsProvider.generate(description, voice, true);
        
        List<Mono<NarrationSegment>> commentMonos = new ArrayList<>();
        if (comments.isArray()) {
            for (JsonNode commentNode : comments) {
                String commentText = commentNode.get("text").asText();
                // TODO: Allow for per-comment voice overrides from the JsonNode
                commentMonos.add(ttsProvider.generate(commentText, voice, true));
            }
        }

        // Execute all TTS API calls in parallel
        return Mono.zip(titleMono, descriptionMono, Flux.concat(commentMonos).collectList())
            .flatMap(tuple -> {
                NarrationSegment titleNarration = tuple.getT1();
                NarrationSegment descriptionNarration = tuple.getT2();
                List<NarrationSegment> commentNarrations = tuple.getT3();
                
                // Once all segments are generated, combine them
                return combineAudioTracks(titleNarration, descriptionNarration, commentNarrations);
            }).block(); // Block until the entire process is complete
    }

    /**
     * Combines multiple audio segments into a single audio track using FFmpeg and adjusts timestamps.
     *
     * @param result The result containing all generated narration segments.
     * @return A Mono emitting the final, combined RedditNarration.
     */
    private Mono<RedditNarration> combineAudioTracks(NarrationSegment titleNarration, 
                                                NarrationSegment descriptionNarration, 
                                                List<NarrationSegment> commentNarrations) {
        // Collect all audio file paths in order
        List<Path> audioFiles = new ArrayList<>();
        audioFiles.add(titleNarration.getAudioFilePath());
        audioFiles.add(descriptionNarration.getAudioFilePath());
        commentNarrations.forEach(segment -> audioFiles.add(segment.getAudioFilePath()));

        Path finalAudioPath;
        try {
            finalAudioPath = Files.createTempFile("final-narration-", ".mp3");
        } catch (IOException e) {
            log.error("Failed to create temporary file for final narration", e);
            return Mono.error(e);
        }

        // 1. Build the FFmpeg command
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        audioFiles.forEach(path -> {
            command.add("-i");
            command.add(path.toAbsolutePath().toString());
        });
        command.add("-filter_complex");
        // Create the filter string: e.g., "[0:a][1:a][2:a]concat=n=3:v=0:a=1[a]"
        String filter = audioFiles.stream()
            .map(path -> "[" + audioFiles.indexOf(path) + ":a]")
            .collect(Collectors.joining()) + "concat=n=" + audioFiles.size() + ":v=0:a=1[a]";
        command.add(filter);
        command.add("-map");
        command.add("[a]");
        command.add("-y"); // Overwrite output file if it exists
        command.add(finalAudioPath.toAbsolutePath().toString());

        log.info("Executing FFmpeg command: {}", String.join(" ", command));

        // 2. Execute the command
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();
            
            // It's crucial to consume the output streams to prevent the process from hanging
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                errorReader.lines().forEach(log::error);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return Mono.error(new IOException("FFmpeg process exited with non-zero code: " + exitCode));
            }
            log.info("FFmpeg successfully created combined audio at: {}", finalAudioPath);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore the interrupted status
            log.error("Failed to execute FFmpeg command", e);
            return Mono.error(e);
        }

        // 3. Adjust all timestamps
        List<WordTiming> combinedTimings = new ArrayList<>();
        double currentOffset = 0.0;

        combinedTimings.addAll(adjustTimings(titleNarration.getWordTimings(), currentOffset));
        // Use the precise end time of the last word for the offset
        //currentOffset += titleNarration.getLastWordEndTime();
        currentOffset += titleNarration.getDurationSeconds();

        combinedTimings.addAll(adjustTimings(descriptionNarration.getWordTimings(), currentOffset));
        //currentOffset += descriptionNarration.getLastWordEndTime();
        currentOffset += descriptionNarration.getDurationSeconds();

        for (NarrationSegment commentNarration : commentNarrations) {
            combinedTimings.addAll(adjustTimings(commentNarration.getWordTimings(), currentOffset));
            //currentOffset += commentNarration.getLastWordEndTime();
            currentOffset += commentNarration.getDurationSeconds();
        }
        
        log.debug("Successfully adjusted {} word timings for the combined track.", combinedTimings.size());
        
        // Clean up the individual segment files now that they've been merged
        audioFiles.forEach(this::deleteTemporaryFile);

        return Mono.just(new RedditNarration(finalAudioPath, currentOffset, combinedTimings, titleNarration.getDurationSeconds()));
    }

    private List<WordTiming> adjustTimings(List<WordTiming> timings, double offset) {
        if (timings == null || timings.isEmpty()) {
            return new ArrayList<>();
        }
        return timings.stream()
            .map(timing -> new WordTiming(
                timing.getWord(),
                timing.getStartTimeSeconds() + offset,
                timing.getEndTimeSeconds() + offset
            ))
            .collect(Collectors.toList());
    }
    
    private void deleteTemporaryFile(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete temporary audio file: {}", path, e);
        }
    }
}

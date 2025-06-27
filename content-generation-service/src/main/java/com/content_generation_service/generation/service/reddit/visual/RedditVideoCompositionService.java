package com.content_generation_service.generation.service.reddit.visual;

import com.content_generation_service.generation.model.RedditNarration;
import com.content_generation_service.generation.service.visual.VideoCompositionBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Responsible for combining all assets (video, audio, images, subtitles)
 * into a final video file using FFmpeg.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedditVideoCompositionService {

    private static final int TARGET_WIDTH = 1080; // Standard width for vertical video

    /**
     * Takes all the generated assets and composes the final video.
     *
     * @param backgroundVideo The path to the background video.
     * @param audioTrack      The path to the narration audio track.
     * @param titleImage      The path to the title image.
     * @param subtitleFile    The path to the .ass subtitle file.
     * @param titleDuration   The duration in seconds to show the title image.
     * @param params          The original JsonNode containing aspect ratio and other settings.
     * @return The path to the final, rendered MP4 video.
     */
    public Path composeVideo(Path backgroundVideo, RedditNarration narration, Path titleImage, Path subtitleFile, JsonNode params) {
        log.info("Composing final video from all assets.");

        Path audioTrack = narration.getAudioFilePath();
        double titleDuration = narration.getTitleDurationSeconds();
        double narrationDuration = narration.getDurationSeconds();
        // --- 1. Calculate Video Dimensions ---
        String aspectRatio = params.get("aspectRatio").asText("9:16");
        String[] parts = aspectRatio.split(":");
        int aspectWidth = Integer.parseInt(parts[0]);
        int aspectHeight = Integer.parseInt(parts[1]);
        int targetHeight = (int) Math.round((double) TARGET_WIDTH / aspectWidth * aspectHeight);
        if (targetHeight % 2 != 0) {
            targetHeight++; // Ensure height is an even number for codec compatibility
        }
        log.info("Target video dimensions set to {}x{}", TARGET_WIDTH, targetHeight);

        Path finalVideoPath;
        try {
            finalVideoPath = Files.createTempFile("final-video-" + UUID.randomUUID(), ".mp4");
        } catch (IOException e) {
            throw new RuntimeException("Could not create temp file for final video.", e);
        }

        // --- 2. Build the FFmpeg Command ---
        // This command is complex. It scales the background video, overlays the title image for a specific duration,
        // and burns in the styled subtitles.
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-i"); command.add(backgroundVideo.toAbsolutePath().toString()); // Input 0: Background video
        command.add("-i"); command.add(titleImage.toAbsolutePath().toString());      // Input 1: Title image
        command.add("-i"); command.add(audioTrack.toAbsolutePath().toString());      // Input 2: Narration audio
        
        command.add("-filter_complex");
        
        String filter = String.format(Locale.US, // Use Locale.US to ensure decimal points are dots
                "[0:v]scale=%d:%d,setsar=1[bg];" + // Scale background video and name it [bg]
                "[1:v]scale=%d:-1[fg];" +          // Scale title image width, keep aspect ratio, name it [fg]
                "[bg][fg]overlay=x=(W-w)/2:y=(H-h)/2:enable='between(t,0,%.2f)'[v_with_overlay];" + // Overlay title for `titleDuration` seconds
                "[v_with_overlay]ass=filename='%s'[v]", // // Burn in subtitles onto the result
                TARGET_WIDTH, targetHeight, TARGET_WIDTH-100, titleDuration, escapePathForFilter(subtitleFile.toAbsolutePath().toString())
        );
        command.add(filter);
        
        command.add("-map"); command.add("[v]");   // Use the final video stream from the filter
        command.add("-map"); command.add("2:a");   // Use the audio from input 2
        
        command.add("-c:v"); command.add("libx264"); // Use a standard video codec
        command.add("-c:a"); command.add("aac");     // Use a standard audio codec
        command.add("-t"); command.add(String.format(Locale.US, "%.2f", narrationDuration)); // Limit video duration
        command.add("-y"); // Overwrite output file
        command.add(finalVideoPath.toAbsolutePath().toString());

        log.info("Executing FFmpeg command: {}", String.join(" ", command));

        // --- 3. Execute the command ---
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();

            // Use a StringBuilder to capture all error output from FFmpeg
            StringBuilder errorOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                reader.lines()
                    .forEach(line -> {
                        // Print FFmpeg's output directly to your console for immediate feedback
                        System.err.println("FFMPEG_DEBUG: " + line);
                        errorOutput.append(line).append("\n");
                    });
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                // Throw an exception that includes the full, detailed error from FFmpeg
                throw new IOException("FFmpeg process exited with code " + exitCode + ". Full error output:\n" + errorOutput);
            }
            log.info("FFmpeg successfully composed final video at: {}", finalVideoPath);

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Video composition failed.", e);
        }
        
        // Clean up temporary files (like the .ass file)
        try { Files.deleteIfExists(subtitleFile); } catch (IOException e) { log.warn("Could not delete temp subtitle file: {}", subtitleFile); }

        return finalVideoPath;
    }

    /**
     * Helper to correctly escape a file path for use in an FFmpeg filter string.
     * This version converts backslashes to forward slashes and escapes special
     * filter characters like ':' and '\'.
     * @param path The absolute path to the file.
     * @return A correctly escaped string for the filter.
     */
    private String escapePathForFilter(String path) {
        // Convert all backslashes to forward slashes for universal compatibility.
        // Then, escape special filter characters: '\', and ':'
        return path.replace("\\", "/")
                .replace(":", "\\:");
    }

    /**
     * Creates and returns a new instance of the VideoCompositionBuilder.
     * @return A fresh VideoCompositionBuilder ready to be configured.
     */
    public VideoCompositionBuilder createComposition() {
        log.info("Creating a new video composition builder.");
        return new VideoCompositionBuilder();
    }
}

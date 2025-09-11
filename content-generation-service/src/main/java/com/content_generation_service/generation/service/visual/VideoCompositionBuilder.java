package com.content_generation_service.generation.service.visual;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.content_generation_service.generation.model.VideoMetadata;

/**
 * A builder for creating and executing complex FFmpeg video compositions.
 * This class allows for a step-by-step construction of an FFmpeg command.
 */
@Component
@Scope("prototype")
@Slf4j
public class VideoCompositionBuilder {

    private final List<Path> inputs = new ArrayList<>();
    private final List<String> filterComplexParts = new ArrayList<>();
    private final List<Path> tempFilesToClean = new ArrayList<>();
    private final List<String> outputOptions = new ArrayList<>();
    private final List<String> inputOptions = new ArrayList<>();
    private final MediaMetadataService videoMetadataService = new MediaMetadataService(new com.fasterxml.jackson.databind.ObjectMapper());

    // Store paths for duration calculation
    private Path backgroundVideoPath = null;
    private Path narrationAudioPath = null;
    
    private String lastVideoStreamTag = "[0:v]";
    private Integer narrationInputIndex = null;
    private double outputDurationSeconds = -1.0; // To store the target output duration

    private int height;
    private int width;

    // Progress listener
    private ProgressListener progressListener;

    // Pattern to extract time from FFmpeg progress output
    private static final Pattern FFMPEG_TIME_PATTERN = Pattern.compile("time=(\\d{2}:\\d{2}:\\d{2}\\.\\d{2})");

    public VideoCompositionBuilder() throws IOException {
        // Default output codecs
        this.outputOptions.add("-c:v");
        this.outputOptions.add("libx264");
        this.outputOptions.add("-pix_fmt");
        this.outputOptions.add("yuv420p");
        this.outputOptions.add("-c:a");
        this.outputOptions.add("aac");
        this.outputOptions.add("-y"); // Overwrite output file
    }

    // Setter for the progress listener
    public VideoCompositionBuilder withProgressListener(ProgressListener listener) {
        this.progressListener = listener;
        return this;
    }

    public VideoCompositionBuilder withDimensions(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public VideoCompositionBuilder withBackground(Path videoPath) {
        this.backgroundVideoPath = videoPath;
        this.inputs.add(videoPath);
        String filter = String.format(Locale.US, "[0:v]setpts=PTS-STARTPTS,scale=%d:%d:force_original_aspect_ratio=increase,crop=%d:%d,setsar=1[bg]",
                width, height, width, height);
        this.filterComplexParts.add(filter);
        this.lastVideoStreamTag = "[bg]";
        return this;
    }

    public VideoCompositionBuilder withNarration(Path audioPath) {
        this.narrationAudioPath = audioPath;
        this.narrationInputIndex = this.inputs.size();
        this.inputs.add(audioPath);
        return this;
    }

    /**
     * Generic method to add any image overlay.
     *
     * @param imagePath     Path to the image file.
     * @param x             The x-coordinate for the top-left corner of the overlay.
     * @param y             The y-coordinate for the top-left corner of the overlay.
     * @param startTime     The time in seconds when the overlay should appear.
     * @param duration      The duration in seconds the overlay should be visible.
     * @param scaleToFit    If true, scales the image to the video's width.
     */
    public VideoCompositionBuilder withImageOverlay(Path imagePath, int x, int y, double startTime, double duration, boolean scaleToFit) {
        int imageInputIndex = this.inputs.size();
        this.inputs.add(imagePath);
        
        String imageStreamTag = "[" + imageInputIndex + ":v]";
        String streamToOverlayTag = imageStreamTag;

        if (scaleToFit) {
            log.debug("Scaling overlay image to fit video width.");
            String scaledImageTag = "[scaled_img" + imageInputIndex + "]";
            // Use -2 to maintain aspect ratio when scaling to a fixed width
            String scaleFilter = String.format(Locale.US, "%sscale=%d:-2%s", imageStreamTag, this.width, scaledImageTag);
            this.filterComplexParts.add(scaleFilter);
            streamToOverlayTag = scaledImageTag;
        }

        String currentVideoTag = this.lastVideoStreamTag;
        String overlayTag = "[ovr" + imageInputIndex + "]";
        double endTime = startTime + duration;

        // The FFmpeg filter now uses the x, y, startTime, and endTime parameters
        String overlayFilter = String.format(Locale.US, "%s%soverlay=%d:%d:enable='between(t,%.2f,%.2f)'%s",
                currentVideoTag, streamToOverlayTag, x, y, startTime, endTime, overlayTag);
        
        this.filterComplexParts.add(overlayFilter);
        this.lastVideoStreamTag = overlayTag;
        return this;
    }

    /**
     * A convenience method for the common use case of adding a centered overlay.
     * It calls the more generic withImageOverlay method internally.
     */
    public VideoCompositionBuilder withCenteredOverlay(Path imagePath, double startTime, double duration, boolean scaleToFit) {
        // FFmpeg's overlay filter can center automatically using these expressions for x and y
        String xExpression = "(W-w)/2";
        String yExpression = "(H-h)/2";
        
        // This logic is now almost identical to the generic one, just with different x/y values.
        int imageInputIndex = this.inputs.size();
        this.inputs.add(imagePath);
        String imageStreamTag = "[" + imageInputIndex + ":v]";
        String streamToOverlayTag = imageStreamTag;

        if (scaleToFit) {
            String scaledImageTag = "[scaled_img" + imageInputIndex + "]";
            String scaleFilter = String.format(Locale.US, "%sscale=%d:-2%s", imageStreamTag, this.width, scaledImageTag);
            this.filterComplexParts.add(scaleFilter);
            streamToOverlayTag = scaledImageTag;
        }

        String currentVideoTag = this.lastVideoStreamTag;
        String overlayTag = "[ovr_centered" + imageInputIndex + "]";
        double endTime = startTime + duration;

        String overlayFilter = String.format(Locale.US, "%s%soverlay=%s:%s:enable='between(t,%.2f,%.2f)'%s",
                currentVideoTag, streamToOverlayTag, xExpression, yExpression, startTime, endTime, overlayTag);

        this.filterComplexParts.add(overlayFilter);
        this.lastVideoStreamTag = overlayTag;
        return this;
    }

    public VideoCompositionBuilder withSubtitles(Path fontDirsPath, Path subtitleFilePath) {
        if (filterComplexParts.isEmpty()) {
            throw new IllegalStateException("Subtitles can only be added after a video stream has been defined.");
        }
        this.tempFilesToClean.add(subtitleFilePath);
        String escapedPath = escapePathForFilter(subtitleFilePath.toAbsolutePath().toString());

        // Get the video stream tag from the LAST operation (which includes all overlays)
        String currentVideoTag = this.lastVideoStreamTag;
        String newOutputTag = "[v_with_subs]"; // A new, final tag for the video stream

        // Create a new, separate filter for the subtitles
        String subtitleFilter = String.format(Locale.US, "%sass=filename='%s':fontsdir='%s'%s",
                currentVideoTag, escapedPath, fontDirsPath.toAbsolutePath().toString(), newOutputTag);
        // Add this filter as the NEW last step in the chain
        this.filterComplexParts.add(subtitleFilter);
        
        // Update the final video tag to point to the stream with subtitles
        this.lastVideoStreamTag = newOutputTag;
        
        return this;
    }

    public VideoCompositionBuilder withOutputDuration(double durationSeconds) {
        this.outputDurationSeconds = durationSeconds;
        int tIndex = this.outputOptions.indexOf("-t");
        if (tIndex != -1 && tIndex + 1 < this.outputOptions.size()) {
            this.outputOptions.remove(tIndex + 1);
            this.outputOptions.remove(tIndex);
        }
        
        this.outputOptions.add("-t");
        this.outputOptions.add(String.format(Locale.US, "%.3f", durationSeconds));
        return this;
    }
    
    public Path buildAndExecute(Path baseSavePath) throws IOException, InterruptedException {
        //Path finalVideoPath = Files.createTempFile(baseSavePath + "final-video-" + UUID.randomUUID(), ".mp4");
        // create the final video path with a unique name
        Files.createDirectories(baseSavePath); // Ensure the base path exists
        if (!Files.isDirectory(baseSavePath)) {
            throw new IllegalArgumentException("Base save path must be a directory: " + baseSavePath);
        }
        Path finalVideoPath = baseSavePath.resolve("final-video-" + UUID.randomUUID() + ".mp4");

        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        
        // Add global input options
        command.addAll(this.inputOptions);

        // Randomization Logic
        double backgroundStartTime = 0.0;
        if (this.backgroundVideoPath != null && this.narrationAudioPath != null) {
            VideoMetadata backgroundMetadata = videoMetadataService.getVideoMetadata(this.backgroundVideoPath);
            double backgroundDuration = backgroundMetadata.duration();

            double narrationDuration = videoMetadataService.getAudioDuration(this.narrationAudioPath);


            if (backgroundDuration > 0 && narrationDuration > 0) {
                double maxStartTime = backgroundDuration - narrationDuration;
                if (maxStartTime > 0) {
                    backgroundStartTime = new Random().nextDouble() * maxStartTime;
                    log.debug("Randomizing background start. Max possible start time: {:.2f}s. Chosen start time: {:.2f}s", maxStartTime, backgroundStartTime);
                } else {
                    log.warn("Narration duration ({:.2f}s) is longer than or equal to background duration ({:.2f}s). Starting background from the beginning.", narrationDuration, backgroundDuration);
                }
            }
        }
        
        // Command construction to handle randomized start time
        // Add background video input with the -ss (seek) option
        if (this.backgroundVideoPath != null) {
            command.add("-ss");
            command.add(String.format(Locale.US, "%.3f", backgroundStartTime));
            command.add("-i");
            command.add(this.backgroundVideoPath.toAbsolutePath().toString());
        }

        // Add all file-based inputs
        for (Path input : this.inputs) {
            // Skip the background path since we already added it
            if (input.equals(this.backgroundVideoPath)) {
                continue;
            }
            command.add("-i");
            command.add(input.toAbsolutePath().toString());
        }

        // Add virtual inputs (like silent audio) if necessary
        boolean hasNarration = this.narrationInputIndex != null;
        if (!hasNarration) {
            log.warn("No narration provided. Adding silent audio track to ensure compatibility.");
            command.add("-f");
            command.add("lavfi");
            command.add("-i");
            command.add("anullsrc=channel_layout=stereo:sample_rate=44100");
        }

        // Add the filter complex chain
        if (!filterComplexParts.isEmpty()) {
            command.add("-filter_complex");
            command.add(String.join(";", this.filterComplexParts));
        }
        
        // --- MAPPING SECTION ---
        // All mapping commands must come after all inputs.
        
        // Map video stream
        command.add("-map");
        command.add(filterComplexParts.isEmpty() ? "0:v" : this.lastVideoStreamTag);
        
        // Map audio stream
        command.add("-map");
        if (hasNarration) {
            command.add(this.narrationInputIndex + ":a");
        } else {
            // The silent audio is the last input. Its index is inputs.size().
            command.add(this.inputs.size() + ":a");
        }

        // Add output options and final path
        command.addAll(this.outputOptions);
        command.add("-shortest"); // Ensure output duration matches shortest stream (video or audio)
        command.add(finalVideoPath.toAbsolutePath().toString());

        log.debug("Executing FFmpeg command: {}", String.join(" ", command));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Process process = processBuilder.start();

        // Use a single-threaded executor for reading stderr to avoid blocking the main thread
        StringBuilder errorOutput = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                //log.debug("FFMPEG: " + line);
                errorOutput.append(line).append("\n");

                // Parse progress
                Matcher matcher = FFMPEG_TIME_PATTERN.matcher(line);
                if (matcher.find()) {
                    String timeString = matcher.group(1);
                    double currentTimeSeconds = parseTimeToSeconds(timeString);
                    
                    // Calculate progress percentage
                    if (outputDurationSeconds > 0 && progressListener != null) {
                        double percentage = (currentTimeSeconds / outputDurationSeconds) * 100.0;
                        percentage = Math.min(100.0, Math.max(0.0, percentage)); // Clamp between 0 and 100
                        progressListener.onProgress(percentage);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error reading FFmpeg stderr: {}", e.getMessage(), e);
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("FFmpeg process exited with code " + exitCode + ". Full error output:\n" + errorOutput);
        }

        log.debug("FFmpeg successfully composed final video at: {}", finalVideoPath);
        cleanupTempFiles();
        return finalVideoPath;
    }

    private void cleanupTempFiles() {
        for (Path path : tempFilesToClean) {
            try {
                Files.deleteIfExists(path);
                log.debug("Cleaned up temporary file: {}", path);
            } catch (IOException e) {
                log.warn("Could not delete temp file: {}", path, e);
            }
        }
    }

    private String escapePathForFilter(String path) {
        return path.replace("\\", "/").replace(":", "\\:");
    }

    // Helper method to parse HH:MM:SS.ms to seconds
    private double parseTimeToSeconds(String timeString) {
        try {
            String[] parts = timeString.split(":");
            double hours = Double.parseDouble(parts[0]);
            double minutes = Double.parseDouble(parts[1]);
            double seconds = Double.parseDouble(parts[2]);
            return (hours * 3600) + (minutes * 60) + seconds;
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            log.error("Failed to parse time string: {}", timeString, e);
            return 0.0; // Return 0 or throw an exception based on desired error handling
        }
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }
}

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
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A builder for creating and executing complex FFmpeg video compositions.
 * This class allows for a step-by-step construction of an FFmpeg command.
 */
@Slf4j
public class VideoCompositionBuilder {

    private final List<Path> inputs = new ArrayList<>();
    private final List<String> filterComplexParts = new ArrayList<>();
    private final List<Path> tempFilesToClean = new ArrayList<>();
    private final List<String> outputOptions = new ArrayList<>();
    private final List<String> inputOptions = new ArrayList<>();
    
    private String lastVideoStreamTag = "[0:v]";
    private Integer narrationInputIndex = null;
    private double outputDurationSeconds = -1.0; // To store the target output duration

    // Progress listener
    private ProgressListener progressListener;

    // Pattern to extract time from FFmpeg progress output
    private static final Pattern FFMPEG_TIME_PATTERN = Pattern.compile("time=(\\d{2}:\\d{2}:\\d{2}\\.\\d{2})");


    public VideoCompositionBuilder() {
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

    public VideoCompositionBuilder withBackground(Path videoPath, int width, int height) {
        this.inputs.add(videoPath);
        String filter = String.format(Locale.US, "[0:v]scale=%d:%d:force_original_aspect_ratio=increase,crop=%d:%d,setsar=1[bg]",
                width, height, width, height);
        this.filterComplexParts.add(filter);
        this.lastVideoStreamTag = "[bg]";
        return this;
    }

    public VideoCompositionBuilder withNarration(Path audioPath) {
        this.narrationInputIndex = this.inputs.size();
        this.inputs.add(audioPath);
        return this;
    }

    /**
     * Overlays an image on top of the current video stream.
     * @param imagePath The path to the image to overlay.
     * @param durationSeconds The duration the overlay should be visible.
     * @param scaleToFit If true, scales the image to fit the video width. If false, uses the image's original size.
     * @return This builder instance for chaining.
     */
    public VideoCompositionBuilder withOverlay(Path imagePath, double durationSeconds, boolean scaleToFit) {
        int imageInputIndex = this.inputs.size();
        
        if (filterComplexParts.isEmpty() && this.inputs.isEmpty()) {
            log.info("No background set. Treating first image as the base video layer.");
            this.inputOptions.add("-loop");
            this.inputOptions.add("1");
            this.inputs.add(imagePath);
            
            String imageStreamTag = "[0:v]";
            String finalTag = "[base_img0]";
            String baseFilter;

            if (scaleToFit) {
                baseFilter = String.format(Locale.US, "%sscale=1080:-2,setsar=1%s", imageStreamTag, finalTag);
            } else {
                baseFilter = String.format(Locale.US, "%ssetsar=1%s", imageStreamTag, finalTag);
            }
            this.filterComplexParts.add(baseFilter);
            this.lastVideoStreamTag = finalTag;
        } else {
            this.inputs.add(imagePath);
            String imageStreamTag = "[" + imageInputIndex + ":v]";
            String streamToOverlayTag = imageStreamTag; // Default to original image stream

            String currentVideoTag = this.lastVideoStreamTag;
            String overlayTag = "[ovr" + imageInputIndex + "]";
            
            if (scaleToFit) {
                log.debug("Scaling overlay image to fit video width.");
                String scaledImageTag = "[scaled_img" + imageInputIndex + "]";
                String scaleFilter = String.format(Locale.US, "%sscale=1080:-1%s", imageStreamTag, scaledImageTag);
                this.filterComplexParts.add(scaleFilter);
                streamToOverlayTag = scaledImageTag; // Use the newly scaled stream for the overlay
            } else {
                log.debug("Using original size for overlay image.");
            }
            
            String overlayFilter = String.format(Locale.US, "%s%soverlay=(W-w)/2:(H-h)/2:enable='between(t,0,%.2f)'%s",
                    currentVideoTag, streamToOverlayTag, durationSeconds, overlayTag);

            this.filterComplexParts.add(overlayFilter);
            this.lastVideoStreamTag = overlayTag;
        }
        return this;
    }

    public VideoCompositionBuilder withSubtitles(Path subtitlePath) {
        if (filterComplexParts.isEmpty()) {
            throw new IllegalStateException("Subtitles can only be added after a video stream has been defined.");
        }
        this.tempFilesToClean.add(subtitlePath);
        String escapedPath = escapePathForFilter(subtitlePath.toAbsolutePath().toString());

        // FIX: Chain the 'ass' filter to the previous filter using a comma for robustness.
        // This modifies the last added filter string in the list.
        String lastFilter = filterComplexParts.remove(filterComplexParts.size() - 1);
        
        // Remove the old output tag (e.g., "[ovr2]") from the end of the last filter
        int lastBracket = lastFilter.lastIndexOf('[');
        if (lastBracket == -1) {
            throw new IllegalStateException("The last filter is missing a valid output tag.");
        }
        String lastFilterWithoutOutput = lastFilter.substring(0, lastBracket);

        // Create the new chained filter
        String newOutputTag = "[v_sub]";
        String newChainedFilter = String.format(Locale.US, "%s,ass=filename='%s'%s",
                lastFilterWithoutOutput, escapedPath, newOutputTag);

        // Add the new, extended filter back to the list
        filterComplexParts.add(newChainedFilter);
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
        this.outputOptions.add(String.format(Locale.US, "%.2f", durationSeconds));
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

        // Add all file-based inputs
        for (Path input : this.inputs) {
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

        log.info("Executing FFmpeg command: {}", String.join(" ", command));

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

        log.info("FFmpeg successfully composed final video at: {}", finalVideoPath);
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
}

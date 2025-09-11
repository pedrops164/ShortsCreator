package com.content_generation_service.generation.service.visual;

import com.content_generation_service.generation.model.VideoMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaMetadataService {

    private final ObjectMapper objectMapper; // Spring Boot provides this bean

    /**
     * Gets metadata (width, height, duration) for a VIDEO file.
     * Uses the video stream (v:0).
     */
    public VideoMetadata getVideoMetadata(Path videoPath) {
        // This command is specific to video files
        String[] command = {
            "ffprobe", "-v", "error", "-select_streams", "v:0",
            "-show_entries", "stream=width,height,duration",
            "-of", "json", videoPath.toAbsolutePath().toString()
        };

        log.info("Executing ffprobe to get VIDEO metadata for: {}", videoPath.getFileName());
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();

            String jsonOutput = readProcessOutput(process.getInputStream());

            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IOException("ffprobe (video) process timed out");
            }

            if (process.exitValue() != 0) {
                String errorOutput = readProcessOutput(process.getErrorStream());
                throw new IOException("ffprobe (video) exited with code " + process.exitValue() + ": " + errorOutput);
            }

            VideoMetadata.FfprobeOutput output = objectMapper.readValue(jsonOutput, VideoMetadata.FfprobeOutput.class);

            if (output.streams() == null || output.streams().isEmpty()) {
                throw new IOException("Could not parse ffprobe output or find video stream for: " + videoPath);
            }

            VideoMetadata.Stream stream = output.streams().get(0);
            return new VideoMetadata(Double.parseDouble(stream.durationString()), stream.width(), stream.height());

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Failed to get video metadata for {}", videoPath, e);
            throw new RuntimeException("Could not determine video metadata.", e);
        }
    }

    /**
     * Gets the duration for an AUDIO file.
     * Uses the audio stream (a:0) and a simpler output format.
     */
    public double getAudioDuration(Path audioPath) {
        // This command is specific to audio files and gets only the duration
        String[] command = {
            "ffprobe", "-v", "error", "-select_streams", "a:0",
            "-show_entries", "format=duration", // Use format=duration for better reliability
            "-of", "default=noprint_wrappers=1:nokey=1",
            audioPath.toAbsolutePath().toString()
        };

        log.info("Executing ffprobe to get AUDIO duration for: {}", audioPath.getFileName());
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();
            
            String durationStr = readProcessOutput(process.getInputStream());

            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IOException("ffprobe (audio) process timed out");
            }

             if (process.exitValue() != 0) {
                String errorOutput = readProcessOutput(process.getErrorStream());
                throw new IOException("ffprobe (audio) exited with code " + process.exitValue() + ": " + errorOutput);
            }

            if (durationStr == null || durationStr.isBlank()) {
                 throw new IOException("ffprobe did not return a duration for: " + audioPath);
            }

            return Double.parseDouble(durationStr.trim());

        } catch (IOException | InterruptedException | NumberFormatException e) {
            Thread.currentThread().interrupt();
            log.error("Failed to get audio duration for {}", audioPath, e);
            throw new RuntimeException("Could not determine audio duration.", e);
        }
    }

    private String readProcessOutput(java.io.InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            return output.toString();
        }
    }
}
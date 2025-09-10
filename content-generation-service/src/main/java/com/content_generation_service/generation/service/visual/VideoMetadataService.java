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
public class VideoMetadataService {

    private final ObjectMapper objectMapper; // Spring Boot provides this bean

    public VideoMetadata getMetadata(Path videoPath) {
        String command = String.format(
            "ffprobe -v error -select_streams v:0 -show_entries stream=width,height,duration -of json %s",
            videoPath.toAbsolutePath().toString()
        );

        log.info("Executing ffprobe command to get metadata for: {}", videoPath.getFileName());

        try {
            Process process = Runtime.getRuntime().exec(command);
            
            StringBuilder jsonOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonOutput.append(line);
                }
            }

            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroy();
                throw new IOException("ffprobe process timed out");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new IOException("ffprobe exited with non-zero code: " + exitCode);
            }

            VideoMetadata.FfprobeOutput output = objectMapper.readValue(jsonOutput.toString(), VideoMetadata.FfprobeOutput.class);

            if (output.streams() == null || output.streams().isEmpty()) {
                throw new IOException("Could not parse ffprobe output or find video stream.");
            }

            VideoMetadata.Stream stream = output.streams().get(0);
            return new VideoMetadata(Double.parseDouble(stream.durationString()), stream.width(), stream.height());

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Failed to get video metadata for {}", videoPath, e);
            throw new RuntimeException("Could not determine video metadata.", e);
        }
    }
}
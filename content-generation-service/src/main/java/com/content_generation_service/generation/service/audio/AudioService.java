package com.content_generation_service.generation.service.audio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.springframework.stereotype.Service;

import com.content_generation_service.generation.model.NarrationSegment;
import com.content_generation_service.generation.model.WordTiming;
import com.content_generation_service.util.ResourceHelperService;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioService {

    private final ResourceHelperService resourceHelperService;

    /**
     * Calculates the duration of an audio file using the JAudiotagger library.
     * @param audioFile Path to the audio file.
     * @return The duration in seconds.
     */
    public double getAudioDuration(Path audioFile) {
        try {
            AudioFile f = AudioFileIO.read(audioFile.toFile());
            double duration = f.getAudioHeader().getPreciseTrackLength();
            log.debug("Calculated audio duration: {} seconds for file {}", duration, audioFile);
            return duration;
        } catch (Exception e) {
            log.warn("Could not calculate audio duration for file: {}. Falling back to 0. Reason: {}", audioFile, e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Adjusts the start and end times of a list of WordTiming objects by a given offset.
     * @param timings The list of WordTiming objects to adjust.
     * @param offset The time in seconds to add to each start and end time.
     * @return A new list of WordTiming objects with adjusted times.
     */
    public List<WordTiming> adjustTimings(List<WordTiming> timings, double offset) {
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

    /**
     * Combines multiple narration segments into a single audio track using FFmpeg and adjusts timestamps.
     * This method is designed to be called by subclasses with a list of generated narration segments.
     *
     * @param narrationSegments A list of NarrationSegment objects to be combined in order.
     * @return A Mono emitting the final, combined NarrationSegment.
     */
    public Mono<NarrationSegment> combineAudioTracks(List<NarrationSegment> narrationSegments) {
        if (narrationSegments == null || narrationSegments.isEmpty()) {
            return Mono.error(new IllegalArgumentException("Narration segments list cannot be null or empty."));
        }

        List<Path> audioFiles = narrationSegments.stream()
            .map(NarrationSegment::getAudioFilePath)
            .collect(Collectors.toList());

        Path finalAudioPath;
        try {
            finalAudioPath = Files.createTempFile("combined-narration-", ".mp3");
        } catch (IOException e) {
            log.error("Failed to create temporary file for final narration", e);
            return Mono.error(e);
        }

        // 1. Build FFmpeg command
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        audioFiles.forEach(path -> {
            command.add("-i");
            command.add(path.toAbsolutePath().toString());
        });
        command.add("-filter_complex");
        String filter = audioFiles.stream()
                .map(path -> "[" + audioFiles.indexOf(path) + ":a]")
                .collect(Collectors.joining()) + "concat=n=" + audioFiles.size() + ":v=0:a=1[a]";
        command.add(filter);
        command.add("-map");
        command.add("[a]");
        command.add("-y");
        command.add(finalAudioPath.toAbsolutePath().toString());

        log.info("Executing FFmpeg command: {}", String.join(" ", command));

        // 2. Execute the command
        try {
            Process process = new ProcessBuilder(command).start();
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                errorReader.lines().forEach(log::error);
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return Mono.error(new IOException("FFmpeg process exited with non-zero code: " + exitCode));
            }
            log.info("FFmpeg successfully created combined audio at: {}", finalAudioPath);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Failed to execute FFmpeg command", e);
            return Mono.error(e);
        }

        // 3. Adjust all timestamps
        List<WordTiming> combinedTimings = new ArrayList<>();
        double currentOffset = 0.0;

        for (NarrationSegment segment : narrationSegments) {
            combinedTimings.addAll(adjustTimings(segment.getWordTimings(), currentOffset));
            currentOffset += segment.getDurationSeconds();
        }
        
        log.debug("Adjusted {} word timings for the combined track.", combinedTimings.size());

        // 4. Clean up intermediate files
        audioFiles.forEach(resourceHelperService::deleteTemporaryFile);

        // Return a new NarrationSegment representing the combined result
        return Mono.just(new NarrationSegment(finalAudioPath, currentOffset, combinedTimings));
    }
}

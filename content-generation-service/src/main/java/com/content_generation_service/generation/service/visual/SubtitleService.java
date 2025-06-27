package com.content_generation_service.generation.service.visual;

import com.content_generation_service.generation.model.WordTiming;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Creates styled subtitle files in the Advanced SubStation Alpha (.ass) format.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubtitleService {

    /**
     * Creates a styled .ass subtitle file from word timings and style parameters.
     *
     * @param wordTimings A list of words and their start/end times.
     * @param styleParams A JsonNode containing subtitle styling info (font, color, position).
     * @return The path to the generated .ass file.
     */
    public Path createAssFile(List<WordTiming> wordTimings, JsonNode styleParams) {
        log.info("Generating styled .ass subtitle file.");

        // Extract styling parameters from the JsonNode
        String font = styleParams.get("subtitlesFont").asText("Arial");
        String color = styleParams.get("subtitlesColor").asText("#FFFFFF");
        String position = styleParams.get("subtitlesPosition").asText("bottom");

        // Build the .ass file content
        StringBuilder assContent = new StringBuilder();
        appendAssHeader(assContent, font, color, position);
        appendAssEvents(assContent, wordTimings);

        // Write content to a temporary file
        Path assPath;
        try {
            assPath = Files.createTempFile("subtitles-" + UUID.randomUUID(), ".ass");
            Files.writeString(assPath, assContent.toString());
        } catch (IOException e) {
            log.error("Failed to write temporary .ass file", e);
            throw new RuntimeException("Could not create temporary subtitle file", e);
        }

        log.debug("Styled .ass file created at: {}", assPath);
        return assPath;
    }

    private void appendAssHeader(StringBuilder builder, String font, String color, String position) {
        // Alignment uses Numpad notation: 2=bottom-center, 5=middle-center, 8=top-center
        int alignment = switch (position.toLowerCase(Locale.ROOT)) {
            case "top" -> 8;
            case "center" -> 5;
            default -> 2; // bottom
        };

        // Convert hex color #RRGGBB to ASS format &HBBGGRR&
        String assColor = "&H" + color.substring(5, 7) + color.substring(3, 5) + color.substring(1, 3) + "&";

        builder.append("[Script Info]\n");
        builder.append("Title: Generated Subtitles\n");
        builder.append("ScriptType: v4.00+\n\n");
        builder.append("[V4+ Styles]\n");
        builder.append("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding\n");
        // Define our custom style
        builder.append(String.format("Style: Default,%s,18,%s,&H000000FF,&H00000000,&H00000000,1,0,0,0,100,100,0,0,1,2,2,%d,10,10,40,1\n\n",
            font, assColor, alignment));
    }

    private void appendAssEvents(StringBuilder builder, List<WordTiming> wordTimings) {
        builder.append("[Events]\n");
        builder.append("Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n");

        if (wordTimings == null) return;

        for (WordTiming timing : wordTimings) {
            String startTime = formatAssTimestamp(timing.getStartTimeSeconds());
            String endTime = formatAssTimestamp(timing.getEndTimeSeconds());
            String text = timing.getWord();

            builder.append(String.format("Dialogue: 0,%s,%s,Default,,0,0,0,,%s\n", startTime, endTime, text));
        }
    }

    private String formatAssTimestamp(double timeInSeconds) {
        int hours = (int) (timeInSeconds / 3600);
        int minutes = (int) ((timeInSeconds % 3600) / 60);
        int seconds = (int) (timeInSeconds % 60);
        int centiseconds = (int) ((timeInSeconds * 100) % 100);
        return String.format(Locale.ROOT, "%d:%02d:%02d.%02d", hours, minutes, seconds, centiseconds);
    }
}

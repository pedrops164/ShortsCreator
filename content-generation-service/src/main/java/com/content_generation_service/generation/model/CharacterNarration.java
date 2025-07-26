package com.content_generation_service.generation.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.nio.file.Path;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class CharacterNarration extends NarrationSegment {
    
    private final List<DialogueLineInfo> dialogueTimings;

    public CharacterNarration(
            Path audioFilePath, 
            double durationSeconds, 
            List<WordTiming> wordTimings, 
            List<DialogueLineInfo> dialogueTimings) {
        super(audioFilePath, durationSeconds, wordTimings);
        this.dialogueTimings = dialogueTimings;
    }
}
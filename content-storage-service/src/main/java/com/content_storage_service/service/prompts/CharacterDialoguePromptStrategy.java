package com.content_storage_service.service.prompts;

import java.util.Map;

import com.content_storage_service.enums.GenerationType;
import org.springframework.stereotype.Component;

@Component
public class CharacterDialoguePromptStrategy implements PromptStrategy {
    @Override
    public GenerationType getGenerationType() {
        return GenerationType.CHARACTER_DIALOGUE;
    }

    @Override
    public String createPrompt(Map<String, String> context) {
        String char1 = context.get("character1Name");
        String char2 = context.get("character2Name");
        String topic = context.get("topic");

        if (char1 == null || char2 == null || topic == null) {
            throw new IllegalArgumentException("Context must contain 'character1Name', 'character2Name', and 'topic'.");
        }

        return String.format(
            """
            Your task is to write an engaging and detailed dialogue between two characters: **%s** and **%s**. The topic of their conversation is: **%s**. Generate between 8 to 12 lines of dialogue in total.

            **IMPORTANT**: Your response **MUST** be a valid JSON array of objects.
            - Each object in the array represents a single line of dialogue.
            - Each object **MUST** have exactly two keys:
              1. A `"character"` key, with the value being the character's name (either "%s" or "%s").
              2. A `"line"` key, with the value being their spoken dialogue.

            Do not include any other text, explanations, or markdown formatting.

            Example format:
            [
              {
                "character": "%s",
                "line": "I can't believe we're discussing this again."
              },
              {
                "character": "%s",
                "line": "Well, it's an important topic! We have to resolve it."
              }
            ]
            """,
            char1, char2, topic, char1, char2, char1, char2
        );
    }
}

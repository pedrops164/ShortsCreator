package com.content_storage_service.service.prompts;

import java.util.Map;

import com.content_storage_service.enums.GenerationType;

public interface PromptStrategy {
    /**
     * Returns the type of generation this strategy handles.
     * @return The GenerationType enum constant.
     */
    GenerationType getGenerationType();

    /**
     * Constructs the specific prompt to be sent to the LLM.
     * @param context A map containing necessary data for the prompt.
     * @return A string representing the fully formed prompt.
     */
    String createPrompt(Map<String, String> context);
}
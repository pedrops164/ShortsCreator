package com.content_generation_service.generation.service;

import com.content_generation_service.client.OpenAiLlmClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class CharacterDialogueEnrichmentService {

    private final OpenAiLlmClient openAiLlmClient;
    private final ObjectMapper objectMapper;
    
    /**
     * Enriches the given character dialogue JSON by adding relevant Google Images search queries
     * to each line of dialogue using an LLM.
     *
     * @param initialDialogue The initial JSON dialogue as a JsonNode.
     * @return A Mono emitting the enriched JsonNode with added "query_list" attributes.
     */
    public Mono<JsonNode> enrichDialogueWithSearchQueries(JsonNode initialDialogue) {
        String prompt = buildPrompt(initialDialogue);
        log.debug("Sending dialogue to LLM for enrichment...");

        // Delegate the API call to the dedicated client
        return openAiLlmClient.call(prompt)
            .flatMap(this::parseJsonString)
            .doOnSuccess(result -> log.debug("Successfully enriched dialogue from LLM."))
            .doOnError(e -> log.error("Failed to enrich dialogue from LLM", e));
    }

    private String buildPrompt(JsonNode dialogue) {
        // This prompt-building logic remains the same, as it's specific to this service's task.
        return """
        You are an assistant that processes JSON. Respond ONLY with the JSON content, without any commentary, explanations, or markdown formatting.
        
        Given the following JSON dialogue array, add a new attribute to each object called "query_list".
        This attribute must be an array of strings. Each string should be a concise, relevant Google Images search query for the content of the "line".
        
        Rules:
        - Generate 1-2 queries for explanatory lines.
        - For simple conversational fillers or questions (e.g., "Wow, that's wild.", "Really?"), return an empty array `[]`.
        - The queries should be designed to find visually engaging images or diagrams.

        Example:
        Input:
        [
          {
            "character": "Professor",
            "line": "Quantum entanglement is a phenomenon where two particles become linked."
          },
          {
            "character": "Student",
            "line": "Wow, that's wild."
          }
        ]
        
        Expected Output:
        [
          {
            "character": "Professor",
            "line": "Quantum entanglement is a phenomenon where two particles become linked.",
            "query_list": ["quantum entanglement diagram", "linked particles visualization"]
          },
          {
            "character": "Student",
            "line": "Wow, that's wild.",
            "query_list": []
          }
        ]

        Now, process this input:
        """ + dialogue.toPrettyString();
    }

    /**
     * Parses the JSON string response from the LLM into a JsonNode.
     *
     * @param jsonString The raw JSON string from the LLM.
     * @return A Mono emitting the parsed JsonNode, or an error if parsing fails.
     */
    private Mono<JsonNode> parseJsonString(String jsonString) {
        try {
            return Mono.just(objectMapper.readTree(jsonString));
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON string from LLM response: {}", jsonString, e);
            // Propagate the error in the reactive chain
            return Mono.error(new RuntimeException("Error parsing LLM JSON response", e));
        }
    }
}
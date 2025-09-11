package com.content_storage_service.service;

import com.content_storage_service.model.Content;
import com.fasterxml.jackson.databind.JsonNode;
import com.shortscreator.shared.dto.ContentPriceV1;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PriceCalculationService {

    private static final String CURRENCY = "USD"; // Default currency

    @Value("${app.pricing.video-generation.base}")
    private int basePriceCents;

    @Value("${app.pricing.video-generation.per-1000-chars}")
    private int centsPer1kCharacters;

    /**
     * Routes the content object to the correct pricing function based on its templateId.
     */
    public ContentPriceV1 calculatePrice(Content content) {
        if (content.getTemplateParams() == null) {
            throw new IllegalArgumentException("Template parameters cannot be null for price calculation.");
        }
        
        return switch (content.getTemplateId()) {
            case "reddit_story_v1" -> calculateRedditStoryPrice(content.getTemplateParams());
            case "character_explains_v1" -> calculateCharacterExplainsPrice(content.getTemplateParams());
            default -> throw new IllegalArgumentException("Pricing not available for template: " + content.getTemplateId());
        };
    }

    /**
     * Calculates price for the "reddit_story_v1" template.
     * Counts characters from title, description (selftext), and all comments.
     */
    private ContentPriceV1 calculateRedditStoryPrice(JsonNode params) {
        int totalChars = 0;

        // Safely access fields using .path() which prevents NullPointerExceptions
        totalChars += params.path("postTitle").asText("").length();
        totalChars += params.path("postDescription").asText("").length();

        if (params.path("comments").isArray()) {
            for (JsonNode commentNode : params.path("comments")) {
                totalChars += commentNode.path("text").asText("").length();
            }
        }
        
        return calculatePriceFromChars(totalChars);
    }

    /**
     * Calculates price for the "character_explains_v1" template.
     * Counts characters from the dialog text only.
     */
    private ContentPriceV1 calculateCharacterExplainsPrice(JsonNode params) {
        int totalChars = 0;

        if (params.path("dialogue").isArray()) {
            for (JsonNode dialogNode : params.path("dialogue")) {
                totalChars += dialogNode.path("text").asText("").length();
            }
        }
        
        return calculatePriceFromChars(totalChars);
    }
    
    /**
     * Core calculation based on total characters.
     * Price is 7 cents per 1000 characters, rounded up.
     */
    private ContentPriceV1 calculatePriceFromChars(int totalChars) {
        if (totalChars == 0) {
            return buildPriceResponse(0);
        }

        // Calculate the price. The cast to double is important for precision before ceiling.
        double price = ((double) totalChars / 1000.0) * centsPer1kCharacters;

        // Math.ceil() rounds up to the nearest whole number, fulfilling the "round up" requirement.
        int finalPriceInCents = basePriceCents + (int) Math.ceil(price);
        
        return buildPriceResponse(finalPriceInCents);
    }

    /**
     * Helper to build the final response object.
     */
    private ContentPriceV1 buildPriceResponse(int priceInCents) {
        return new ContentPriceV1(priceInCents, CURRENCY);
    }
}
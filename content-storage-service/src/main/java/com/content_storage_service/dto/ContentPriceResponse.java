package com.content_storage_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentPriceResponse {

    /**
     * The final calculated price for the content generation.
     */
    private Integer finalPrice; // Price in cents

    /**
     * The currency of the price (e.g., "USD", "EUR").
     */
    private String currency;
}
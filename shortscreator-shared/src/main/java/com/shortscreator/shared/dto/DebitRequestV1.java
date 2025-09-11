package com.shortscreator.shared.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * DTO for requesting a debit from the Payment Service.
 * @param userId The ID of the user to charge.
 * @param priceDetails The calculated price and currency.
 * @param contentType The type of content being generated, for transaction logging.
 */
public record DebitRequestV1(
    @NotBlank
    String userId,

    @NotNull
    @Positive
    Integer amountInCents,

    @NotNull
    ChargeReasonV1 reason, // An enum for categorizing the charge.

    @NotBlank
    String idempotencyKey, // Crucial for preventing duplicate charges on retries.

    // Optional flexible map for service-specific context
    Map<String, String> metadata
) {}

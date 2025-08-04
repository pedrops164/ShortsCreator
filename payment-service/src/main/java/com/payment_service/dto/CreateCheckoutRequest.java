package com.payment_service.dto;

import jakarta.validation.constraints.NotBlank;

/* Request DTO for specifying the top-up option the user selected */
public record CreateCheckoutRequest(
    @NotBlank(message = "Package ID cannot be blank")
    String packageId // e.g., "topup_10_usd"
) {}
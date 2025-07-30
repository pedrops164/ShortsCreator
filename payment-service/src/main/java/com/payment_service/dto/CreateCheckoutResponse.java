package com.payment_service.dto;

/* Backend responds with sessionId, and frontend redirects user to payment page */
public record CreateCheckoutResponse(
    String sessionId
) {}
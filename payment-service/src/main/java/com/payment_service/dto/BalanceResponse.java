package com.payment_service.dto;

/* Object returned to frontend when it requests user balance */
public record BalanceResponse(
    long balanceInCents,
    String currency
) {}
package com.payment_service.dto;

/* CGS uses this object to debit user balance*/
public record DebitRequest(
    String userId,
    long amount, // The amount to debit, in cents
    String currency
) {}
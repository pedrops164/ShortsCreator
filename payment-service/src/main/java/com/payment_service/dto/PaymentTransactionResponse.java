package com.payment_service.dto;

import java.time.Instant;
import java.util.UUID;

import com.payment_service.model.TransactionStatus;

public record PaymentTransactionResponse(
    UUID id,
    Integer amountPaid, // in cents
    String currency,
    TransactionStatus status,
    Instant createdAt,
    String paymentIntentId
) {}
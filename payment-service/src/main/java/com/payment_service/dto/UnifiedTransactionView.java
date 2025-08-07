package com.payment_service.dto;

import java.time.Instant;
import java.util.UUID;

import com.payment_service.enums.TransactionType;
import com.shortscreator.shared.enums.TransactionStatus;

// This DTO represents a single item in the user's unified transaction history (deposits and charges).
public record UnifiedTransactionView(
    UUID id,                  // Unique ID of the transaction/charge
    TransactionType type,     // "DEPOSIT" or "CHARGE"
    String description,       // "Balance Top-up" or "Reddit Story Generation"
    Integer amount,           // Always positive. Will be displayed as + or - on the frontend.
    String currency,
    TransactionStatus status, // e.g., "COMPLETED", "REFUNDED", "FAILED"
    Instant createdAt
) {}
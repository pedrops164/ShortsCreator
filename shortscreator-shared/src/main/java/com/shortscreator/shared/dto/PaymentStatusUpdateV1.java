package com.shortscreator.shared.dto;

import java.util.UUID;

import com.shortscreator.shared.enums.TransactionStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentStatusUpdateV1(
    @NotBlank(message = "User ID must not be null or empty.") String userId,
    @NotNull(message = "Transaction ID must not be null.") UUID transactionId,
    @NotNull(message = "Amount paid must not be null.") Integer amountPaid, // in cents
    @NotNull(message = "Status must not be null.") TransactionStatus status
) {}
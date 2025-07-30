package com.payment_service.model;

/**
 * Represents the status of a payment transaction.
 */
public enum TransactionStatus {
    COMPLETED,
    PENDING,
    FAILED,
    REFUNDED,
    DISPUTED
}
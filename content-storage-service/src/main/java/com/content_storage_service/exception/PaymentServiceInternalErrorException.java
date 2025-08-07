package com.content_storage_service.exception;

// This exception means the Payment Service is down or has an internal error (e.g., 5xx).
public class PaymentServiceInternalErrorException extends RuntimeException {
    public PaymentServiceInternalErrorException(String message) {
        super(message);
    }
}
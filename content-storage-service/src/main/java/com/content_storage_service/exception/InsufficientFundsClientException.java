package com.content_storage_service.exception;

// This exception specifically means the payment failed due to lack of funds.
public class InsufficientFundsClientException extends RuntimeException {
    public InsufficientFundsClientException(String message) {
        super(message);
    }
}
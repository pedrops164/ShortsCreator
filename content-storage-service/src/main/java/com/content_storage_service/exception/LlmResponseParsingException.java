package com.content_storage_service.exception;

public class LlmResponseParsingException extends RuntimeException {
    public LlmResponseParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
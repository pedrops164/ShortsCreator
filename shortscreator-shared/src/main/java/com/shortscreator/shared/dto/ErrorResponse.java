package com.shortscreator.shared.dto;

import java.time.Instant;

/**
 * A standardized error response format for all services.
 */
public record ErrorResponse(
    Instant timestamp,      // When the error occurred
    int status,             // The HTTP status code (e.g., 409)
    String errorCode,       // A "machine-readable" error code (e.g., "INSUFFICIENT_FUNDS")
    String message,         // A human-readable message for the user
    String path             // The request path that caused the error
) {}
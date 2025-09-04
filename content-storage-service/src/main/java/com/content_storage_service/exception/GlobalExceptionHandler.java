package com.content_storage_service.exception;

import com.shortscreator.shared.dto.ErrorResponse;

import org.springframework.http.server.reactive.ServerHttpRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InsufficientFundsClientException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFunds(InsufficientFundsClientException ex, ServerHttpRequest request) {
        String path = request.getURI().getPath();
        // Log with more context: Method and Path
        log.warn("Insufficient Funds ({} {}): {}", request.getMethod(), path, ex.getMessage());
        
        
        ErrorResponse errorResponse = new ErrorResponse(
            Instant.now(),
            HttpStatus.CONFLICT.value(), // 409
            "INSUFFICIENT_FUNDS",        // The machine-readable code for the frontend!
            ex.getMessage(),
            path
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(PaymentServiceInternalErrorException.class)
    public ResponseEntity<ErrorResponse> handlePaymentServiceDown(PaymentServiceInternalErrorException ex, ServerHttpRequest request) {
        String path = request.getURI().getPath();
        // Log with more context and the full exception for deeper insight if needed
        log.error("Payment Service Unavailable ({} {}): {}", request.getMethod(), path, ex.getMessage(), ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
            Instant.now(),
            HttpStatus.SERVICE_UNAVAILABLE.value(), // 503
            "PAYMENT_SERVICE_UNAVAILABLE",
            "The payment service is temporarily down. Please try again later.", // A user-friendly message
            path
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, ServerHttpRequest request) {
        String path = request.getURI().getPath();
        log.warn("Bad Request ({} {}): {}", request.getMethod(), path, ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(), // 400
            "INVALID_ARGUMENT",
            ex.getMessage(),
            path
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex, ServerHttpRequest request) {
        String path = request.getURI().getPath();
        log.warn("Resource State Conflict ({} {}): {}", request.getMethod(), path, ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
            Instant.now(),
            HttpStatus.CONFLICT.value(), // 409
            "RESOURCE_STATE_CONFLICT",
            ex.getMessage(),
            path
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }
    
    /**
     * This is the "catch-all" handler.
     * It catches any exception not handled by the more specific handlers above.
     * This prevents stack traces from being leaked to the user and ensures every
     * single unexpected error is logged properly.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, ServerHttpRequest request) {
        String path = request.getURI().getPath();
        // Always log unexpected exceptions as ERROR and include the full stack trace
        log.error("Unhandled Internal Server Error ({} {}): {}", request.getMethod(), path, ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse(
            Instant.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(), // 500
            "INTERNAL_SERVER_ERROR",
            "An unexpected internal error occurred. Please try again later.", // Hide internal details
            path
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
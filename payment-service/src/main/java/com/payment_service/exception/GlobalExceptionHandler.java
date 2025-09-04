package com.payment_service.exception;

import com.shortscreator.shared.dto.ErrorResponse; // Import from your shared library
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handler for when the user doesn't have enough money
    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFunds(InsufficientFundsException ex, HttpServletRequest request) {
        log.warn("Business rule violation: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            Instant.now(),
            HttpStatus.CONFLICT.value(), // 409
            "INSUFFICIENT_FUNDS",
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    // Handler for when a user's balance record isn't found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
            Instant.now(),
            HttpStatus.NOT_FOUND.value(), // 404
            "USER_BALANCE_NOT_FOUND",
            ex.getMessage(),
            request.getRequestURI()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    // Handler for bad arguments, like currency mismatch
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Invalid argument provided: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(), // 400
            "INVALID_ARGUMENT",
            ex.getMessage(),
            request.getRequestURI()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * This is the "catch-all" handler.
     * It catches any exception not handled by the more specific handlers above.
     * This prevents stack traces from being leaked to the user and ensures every
     * single unexpected error is logged properly.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        String path = request.getRequestURI();
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
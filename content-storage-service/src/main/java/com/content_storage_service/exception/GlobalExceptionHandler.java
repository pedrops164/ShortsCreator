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
        log.warn("Payment failed due to insufficient funds: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            Instant.now(),
            HttpStatus.CONFLICT.value(), // 409
            "INSUFFICIENT_FUNDS",        // The machine-readable code for the frontend!
            ex.getMessage(),
            request.getURI().getPath()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(PaymentServiceInternalErrorException.class)
    public ResponseEntity<ErrorResponse> handlePaymentServiceDown(PaymentServiceInternalErrorException ex, ServerHttpRequest request) {
        log.error("Cannot connect to Payment Service: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            Instant.now(),
            HttpStatus.SERVICE_UNAVAILABLE.value(), // 503
            "PAYMENT_SERVICE_UNAVAILABLE",
            "The payment service is temporarily down. Please try again later.", // A user-friendly message
            request.getURI().getPath()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponse> handleValidationErrors(RuntimeException ex, ServerHttpRequest request) {
        log.warn("Bad request encountered: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(), // 400
            "INVALID_REQUEST",
            ex.getMessage(),
            request.getURI().getPath()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}
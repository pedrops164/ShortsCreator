package com.payment_service.controller;

import com.payment_service.dto.CreateCheckoutRequest;
import com.payment_service.dto.CreateCheckoutResponse;
import com.payment_service.service.PaymentService;
import com.stripe.model.checkout.Session;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Creates a Stripe Checkout session to allow a user to add balance.
     * The user's ID is provided by the API Gateway in a trusted header.
     *
     * @param userId The ID of the user initiating the payment.
     * @param request The request containing the desired top-up package.
     * @return A response containing the Stripe checkout redirect url.
     */
    @PostMapping("/create-checkout-session")
    public ResponseEntity<CreateCheckoutResponse> createCheckoutSession(
            @RequestHeader("X-User-ID") String userId,
            @Valid @RequestBody CreateCheckoutRequest request) {
        
        log.info("Received request to create checkout session for user ID: {} with package: {}",
            userId, request.packageId());

        // Delegate the complex logic of interacting with Stripe to the service layer
        Session session = paymentService.createStripeCheckoutSession(request, userId);

        CreateCheckoutResponse response = new CreateCheckoutResponse(session.getUrl());
        
        return ResponseEntity.ok(response);
    }
}
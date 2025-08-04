package com.payment_service.controller;

import com.payment_service.config.StripeProperties;
import com.payment_service.service.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/stripe")
public class StripeController {

    private final PaymentService paymentService;
    private final StripeProperties stripeProperties;

    public StripeController(PaymentService paymentService, StripeProperties stripeProperties) {
        this.paymentService = paymentService;
        this.stripeProperties = stripeProperties;
    }

    @PostMapping("/webhooks")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        Event event;

        // Verify the signature - CRITICAL FOR SECURITY
        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeProperties.webhookSecret());
        } catch (SignatureVerificationException e) {
            log.warn("Webhook signature verification failed.", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = null;
        if (dataObjectDeserializer.getObject().isPresent()) {
            stripeObject = dataObjectDeserializer.getObject().get();
        } else {
            // Deserialization failed, probably due to an API version mismatch.
            log.error("Could not deserialize Stripe event data for event ID {}", event.getId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Deserialization error");
        }

        // Handle the specific event type
        switch (event.getType()) {
            case "checkout.session.completed":
                Session session = (Session) stripeObject;
                log.info("Received checkout.session.completed event for session ID: {}", session.getId());
                paymentService.handleCheckoutSessionCompleted(session);
                break;
            case "checkout.session.async_payment_succeeded":
                Session succeededSession = (Session) stripeObject;
                paymentService.handleAsyncPaymentSucceeded(succeededSession);
                break;
            case "checkout.session.async_payment_failed":
                Session failedSession = (Session) stripeObject;
                paymentService.handleAsyncPaymentFailed(failedSession);
                break;
            case "charge.dispute.created":
                Charge disputedCharge = (Charge) stripeObject;
                log.info("Received charge.dispute.created for charge ID: {}", disputedCharge.getId());
                paymentService.handleChargeDisputeCreated(disputedCharge);
                break;

            case "charge.refunded":
                Charge refundedCharge = (Charge) stripeObject;
                log.info("Received charge.refunded for charge ID: {}", refundedCharge.getId());
                paymentService.handleChargeRefunded(refundedCharge);
                break;
            default:
                log.warn("Unhandled event type: {}", event.getType());
        }

        return ResponseEntity.ok("");
    }
}
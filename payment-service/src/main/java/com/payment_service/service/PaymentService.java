package com.payment_service.service;

import com.payment_service.config.StripeProperties;
import com.payment_service.dto.CreateCheckoutRequest;
import com.payment_service.model.PaymentTransaction;
import com.payment_service.model.TransactionStatus;
import com.payment_service.model.UserBalance;
import com.payment_service.repository.PaymentTransactionRepository;
import com.payment_service.repository.UserBalanceRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final StripeProperties stripeProperties;
    private final UserBalanceRepository userBalanceRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeProperties.secretKey();
    }

    /**
     * Creates and returns a Stripe Checkout Session.
     *
     * @param request The DTO containing the packageId.
     * @param userId The internal user ID to link to the transaction.
     * @return The created Stripe Session object.
     * @throws IllegalArgumentException if the packageId is not configured.
     * @throws RuntimeException if there is an error communicating with Stripe.
     */
    public Session createStripeCheckoutSession(CreateCheckoutRequest request, String userId) {
        // Look up the Stripe Price ID from our application configuration
        String priceId = stripeProperties.priceMap().get(request.packageId());
        if (priceId == null) {
            log.error("Invalid package ID '{}' requested by user {}", request.packageId(), userId);
            throw new IllegalArgumentException("Invalid package ID provided.");
        }

        // Build the session parameters for the Stripe API call
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(stripeProperties.successUrl())
                .setCancelUrl(stripeProperties.cancelUrl())
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPrice(priceId)
                                .setQuantity(1L)
                                .build())
                // CRITICAL: Add internal user ID to Stripe's metadata
                // This is how we'll identify the user in the webhook after payment.
                .putMetadata("userId", userId)
                .build();

        try {
            // Create the session via the Stripe API
            Session session = Session.create(params);
            log.info("Successfully created Stripe session {} for user {}", session.getId(), userId);
            return session;
        } catch (StripeException e) {
            log.error("Failed to create Stripe session for user {}: {}", userId, e.getMessage());
            // Wrap the checked StripeException in a runtime exception
            throw new RuntimeException("Error creating Stripe checkout session.", e);
        }
    }

    /**
     * Handles a successful checkout session event from a Stripe webhook.
     * This method is transactional and idempotent.
     *
     * @param session The Stripe Session object from the webhook event.
     */
    @Transactional
    public void handleCheckoutSessionCompleted(Session session) {
        // Idempotency Check: Ensure we haven't already processed this payment.
        if (paymentTransactionRepository.existsByStripeCheckoutSessionId(session.getId())) {
            log.info("Webhook for session {} already processed. Skipping.", session.getId());
            return;
        }

        // Get user ID from metadata and find the user's balance record.
        String userId = session.getMetadata().get("userId");
        String paymentCurrency = session.getCurrency().toUpperCase();
        long amountPaid = session.getAmountTotal();

        // Find the user's balance record
        UserBalance userBalance = userBalanceRepository.findByUserId(userId)
                .orElseGet(() -> new UserBalance(userId));

        // Verify the currency
        if (!userBalance.getCurrency().equals(paymentCurrency)) {
            log.error("CRITICAL: Currency mismatch for user {}. Account currency is {} but payment was in {}. Transaction will NOT be processed.",
                userId, userBalance.getCurrency(), paymentCurrency);
            // Do not process the transaction to maintain data integrity.
            return; 
        }

        // Add the funds to the user's balance.
        userBalance.setBalanceInCents(userBalance.getBalanceInCents() + amountPaid);
        userBalanceRepository.save(userBalance);

        // Create and save a transaction record for auditing purposes.
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setUserId(userId);
        transaction.setStripeCheckoutSessionId(session.getId());
        transaction.setPaymentIntentId(session.getPaymentIntent());
        transaction.setAmountPaid(Math.toIntExact(amountPaid));
        transaction.setCurrency(session.getCurrency().toUpperCase());
        transaction.setStatus(TransactionStatus.COMPLETED);
        paymentTransactionRepository.save(transaction);
        
        log.info("Successfully processed payment for user {} from session {}. New balance: {}",
            userId, session.getId(), userBalance.getBalanceInCents());
    }

    /**
     * Handles a chargeback dispute. Finds the original transaction
     * and updates its status to DISPUTED.
     */
    @Transactional
    public void handleChargeDisputeCreated(Charge charge) {
    String paymentIntentId = charge.getPaymentIntent();
    log.warn("Dispute created for charge associated with Payment Intent: {}. Amount: {} {}", 
        paymentIntentId, charge.getAmount(), charge.getCurrency());

    // Find the original transaction to get the user ID
    paymentTransactionRepository.findByPaymentIntentId(paymentIntentId).ifPresent(transaction -> {
        // Find the user's current balance
        userBalanceRepository.findByUserId(transaction.getUserId()).ifPresent(userBalance -> {

            // Deduct the disputed amount from their current balance
            long disputedAmount = charge.getAmount(); // Amount is in cents
            userBalance.setBalanceInCents(userBalance.getBalanceInCents() - disputedAmount);
            userBalanceRepository.save(userBalance);
            
            log.info("Deducted {} from user {}'s balance due to dispute. New balance: {}",
                disputedAmount, userBalance.getUserId(), userBalance.getBalanceInCents());
        });

        // Mark the transaction as disputed and consider locking the user's account
        transaction.setStatus(TransactionStatus.DISPUTED);
        paymentTransactionRepository.save(transaction);
        log.info("Updated transaction {} to DISPUTED. Account should be reviewed.", transaction.getId());
        // TODO: Alert support team
    });
}

    /**
     * Handles a refunded charge. Finds the original transaction
     * and updates its status to REFUNDED.
     */
    @Transactional
    public void handleChargeRefunded(Charge charge) {
        String paymentIntentId = charge.getPaymentIntent();
        log.warn("Refund processed for charge associated with Payment Intent: {}", paymentIntentId);

        // Find the original transaction to get the user ID and amount
        paymentTransactionRepository.findByPaymentIntentId(paymentIntentId).ifPresent(transaction -> {
            // Find the user's current balance
            userBalanceRepository.findByUserId(transaction.getUserId()).ifPresent(userBalance -> {
                
                // Deduct the refunded amount from their current balance
                long refundedAmount = charge.getAmountRefunded(); // Amount is in cents
                userBalance.setBalanceInCents(userBalance.getBalanceInCents() - refundedAmount);
                userBalanceRepository.save(userBalance);
                
                log.info("Deducted {} from user {}'s balance due to refund. New balance: {}",
                    refundedAmount, userBalance.getUserId(), userBalance.getBalanceInCents());
            });

            // Mark the original transaction as refunded for your records
            transaction.setStatus(TransactionStatus.REFUNDED);
            paymentTransactionRepository.save(transaction);
            log.info("Updated transaction {} to REFUNDED status.", transaction.getId());
        });
    }
}
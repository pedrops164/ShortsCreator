package com.payment_service.service;

import com.payment_service.config.StripeProperties;
import com.payment_service.dto.CreateCheckoutRequest;
import com.payment_service.messaging.PaymentStatusDispatcher;
import com.payment_service.model.PaymentTransaction;
import com.payment_service.model.UserBalance;
import com.payment_service.repository.PaymentTransactionRepository;
import com.payment_service.repository.UserBalanceRepository;
import com.shortscreator.shared.dto.PaymentStatusUpdateV1;
import com.shortscreator.shared.enums.TransactionStatus;
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
    private final PaymentStatusDispatcher paymentStatusDispatcher;

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

        // Create and save a transaction record for auditing purposes.
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setUserId(userId);
        transaction.setStripeCheckoutSessionId(session.getId());
        transaction.setPaymentIntentId(session.getPaymentIntent());
        transaction.setAmountPaid(Math.toIntExact(amountPaid));
        transaction.setCurrency(session.getCurrency().toUpperCase());

        // Set the initial status based on the payment type
        if ("paid".equals(session.getPaymentStatus())) {
            log.info("Synchronous payment for session {} completed successfully.", session.getId());
            addBalanceToUser(userId, amountPaid);
            transaction.setStatus(TransactionStatus.COMPLETED);
            // No need to notify frontend here.
        } else {
            log.info("Asynchronous payment for session {} initiated. Status is PENDING.", session.getId());
            transaction.setStatus(TransactionStatus.PENDING);
            // No need to notify frontend here.
        }

        paymentTransactionRepository.save(transaction);
    }

    @Transactional
    public void handleAsyncPaymentSucceeded(Session session) {
        log.info("Asynchronous payment succeeded for session {}.", session.getId());
        paymentTransactionRepository.findByStripeCheckoutSessionId(session.getId()).ifPresentOrElse(transaction -> {
            if (transaction.getStatus() == TransactionStatus.PENDING) {
                transaction.setStatus(TransactionStatus.COMPLETED);
                addBalanceToUser(transaction.getUserId(), session.getAmountTotal());
                // TODO: Notify frontend that the pending payment is now complete.
                PaymentStatusUpdateV1 update = new PaymentStatusUpdateV1(
                    transaction.getUserId(),
                    transaction.getId(),
                    Math.toIntExact(session.getAmountTotal()),
                    TransactionStatus.COMPLETED
                );
                // Publish the update to the message broker
                paymentStatusDispatcher.dispatchPaymentStatus(update);
                paymentTransactionRepository.save(transaction);
            }
        }, () -> {
            log.warn("No transaction found for session {}. Cannot update status.", session.getId());
            // TODO: Alert support team
        });
    }

    @Transactional
    public void handleAsyncPaymentFailed(Session session) {
        String userId = session.getMetadata().get("userId");
        log.warn("Asynchronous payment failed for session {} for user {}.", session.getId(), userId);
        paymentTransactionRepository.findByStripeCheckoutSessionId(session.getId()).ifPresentOrElse(transaction -> {
            transaction.setStatus(TransactionStatus.FAILED);
            paymentTransactionRepository.save(transaction);
            // TODO: Notify frontend that the payment has failed.
            PaymentStatusUpdateV1 update = new PaymentStatusUpdateV1(
                userId,
                transaction.getId(),
                Math.toIntExact(session.getAmountTotal()),
                TransactionStatus.FAILED
            );
            // Publish the update to the message broker
            paymentStatusDispatcher.dispatchPaymentStatus(update);
        }, () -> {
            log.warn("No transaction found for session {}. Cannot update status.", session.getId());
            // TODO: Alert support team
        });
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

    /**
     * Adds the specified amount to the user's balance.
     * If no balance exists, creates a new one.
     *
     * @param userId The internal user ID.
     * @param amountToAdd The amount in cents to add to the user's balance.
     */
    @Transactional
    private void addBalanceToUser(String userId, long amountToAdd) {
        userBalanceRepository.findByUserId(userId).ifPresentOrElse(userBalance -> {
            userBalance.setBalanceInCents(userBalance.getBalanceInCents() + amountToAdd);
            userBalanceRepository.save(userBalance);
            log.info("Added {} cents to user {}'s balance. New balance: {}",
                amountToAdd, userId, userBalance.getBalanceInCents());
        }, () -> {
            // If no balance exists, create a new one
            UserBalance newUserBalance = new UserBalance(userId);
            newUserBalance.setBalanceInCents(amountToAdd);
            userBalanceRepository.save(newUserBalance);
            log.info("Created new balance for user {} with initial amount: {}", userId, amountToAdd);
        });
    }
}
package com.payment_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.payment_service.model.PaymentTransaction;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    /**
     * Checks if a transaction with the given Stripe Checkout Session ID already exists.
     * This is used to prevent processing the same webhook event multiple times.
     * @param sessionId The Stripe Checkout Session ID.
     * @return true if a transaction exists, false otherwise.
     */
    boolean existsByStripeCheckoutSessionId(String sessionId);

    /**
     * Finds a transaction by its Payment Intent ID.
     * @param paymentIntentId The Payment Intent ID.
     * @return The PaymentTransaction if found, null otherwise.
     */
    Optional<PaymentTransaction> findByPaymentIntentId(String paymentIntentId);

    /**
     * Finds a transaction by its Stripe Checkout Session ID.
     * @param sessionId The Stripe Checkout Session ID.
     * @return The PaymentTransaction if found, null otherwise.
     */
    Optional<PaymentTransaction> findByStripeCheckoutSessionId(String sessionId);

    /**
     * Finds all transactions for a given user, ordered by creation date descending.
     * @param userId The ID of the user.
     * @param pageable The pagination information (page number, size).
     * @return A paginated list of transactions.
     */
    Page<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
}
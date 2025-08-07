package com.payment_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.payment_service.dto.UnifiedTransactionProjection;
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

    // This native query combines deposits and charges, orders them, and applies pagination.
    @Query(value = """
        SELECT * FROM (
            -- Query 1: Deposits from payment_transactions
            SELECT id, 'DEPOSIT' as type, 'Balance Top-up' as description, amount_paid as amount, currency, status, created_at
            FROM payment_transactions
            WHERE user_id = :userId
            UNION ALL
            -- Query 2: Charges from generation_charges
            SELECT id, 'CHARGE' as type,
                   CASE 
                       WHEN content_type = 'REDDIT_STORY' THEN 'Reddit Story Generation'
                       WHEN content_type = 'CHARACTER_EXPLAINS' THEN 'Character Explains Generation'
                       ELSE 'Content Generation'
                   END as description,
                   amount, currency, status, created_at
            FROM generation_charges
            WHERE user_id = :userId
        ) as unified_transactions
        ORDER BY created_at DESC
    """,
    countQuery = """
        -- A separate query to get the total count for pagination, which is more efficient.
        SELECT COUNT(*) FROM (
            SELECT id FROM payment_transactions WHERE user_id = :userId
            UNION ALL
            SELECT id FROM generation_charges WHERE user_id = :userId
        ) as count_query
    """,
    nativeQuery = true)
    Page<UnifiedTransactionProjection> findUnifiedTransactionsForUser(@Param("userId") String userId, Pageable pageable);
}
package com.payment_service.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import com.shortscreator.shared.enums.TransactionStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor // Generates the default constructor required by JPA
@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "stripe_checkout_session_id", unique = true, nullable = false)
    private String stripeCheckoutSessionId;

    @Column(name = "amount_paid", nullable = false)
    private Integer amountPaid; // In cents to avoid floating point issues

    @Column(name = "currency", nullable = false)
    private String currency;
    
    // useful to track the payment intent in Stripe
    @Column(name = "payment_intent_id", unique = true, nullable = false)
    private String paymentIntentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    // Getters and Setters
}

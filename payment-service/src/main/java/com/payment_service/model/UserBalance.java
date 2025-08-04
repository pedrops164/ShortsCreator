package com.payment_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor // Generates the default constructor required by JPA
@Entity
@Table(name = "user_balance")
public class UserBalance {

    @Id
    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "balance_in_cents", nullable = false)
    private Long balanceInCents = 0L;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency; // e.g., "USD"

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UserBalance(String userId) {
        this.userId = userId;
        this.currency = "USD"; // Default currency is USD
        this.balanceInCents = 0L;
    }
}
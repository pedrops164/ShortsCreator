package com.payment_service.model;

import com.shortscreator.shared.enums.ChargeStatus;
import com.shortscreator.shared.enums.ContentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "generation_charges")
public class GenerationCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private String userId;

    @Column(name = "content_id", unique = true, nullable = false, updatable = false)
    private String contentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, updatable = false)
    private ContentType contentType;

    @Column(name = "amount", nullable = false, updatable = false)
    private Integer amount; // In cents

    @Column(name = "currency", nullable = false, updatable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ChargeStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
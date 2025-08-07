package com.payment_service.dto;

import java.time.Instant;

// This interface acts as a "view" on the raw SQL result set.
// Spring Data will automatically implement this.
public interface UnifiedTransactionProjection {
    String getId();
    String getType();
    String getDescription();
    Integer getAmount();
    String getCurrency();
    String getStatus();
    Instant getCreatedAt();
}
package com.payment_service.repository;

import com.payment_service.model.GenerationCharge;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface GenerationChargeRepository extends JpaRepository<GenerationCharge, UUID> {
    // We need to find charges by contentId for refunds.
    Optional<GenerationCharge> findByContentId(String contentId);
}
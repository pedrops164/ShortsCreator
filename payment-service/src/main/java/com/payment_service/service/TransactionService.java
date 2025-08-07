package com.payment_service.service;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payment_service.dto.UnifiedTransactionProjection;
import com.payment_service.dto.UnifiedTransactionView;
import com.payment_service.enums.TransactionType;
import com.payment_service.repository.PaymentTransactionRepository;
import com.shortscreator.shared.enums.TransactionStatus;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final PaymentTransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public Page<UnifiedTransactionView> findUnifiedTransactionsByUserId(String userId, Pageable pageable) {
        // Fetch the page of raw projections from the repository.
        Page<UnifiedTransactionProjection> projectionPage = transactionRepository.findUnifiedTransactionsForUser(userId, pageable);
        
        // Map the projection page to our final DTO page.
        return projectionPage.map(this::convertToView);
    }

    // Private helper method to perform the safe conversion.
    private UnifiedTransactionView convertToView(UnifiedTransactionProjection projection) {
        return new UnifiedTransactionView(
            UUID.fromString(projection.getId()),
            TransactionType.valueOf(projection.getType()),
            projection.getDescription(),
            projection.getAmount(),
            projection.getCurrency(),
            TransactionStatus.valueOf(projection.getStatus()),
            projection.getCreatedAt()
        );
    }
}
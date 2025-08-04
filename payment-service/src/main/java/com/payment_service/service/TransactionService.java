package com.payment_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payment_service.dto.PaymentTransactionResponse;
import com.payment_service.model.PaymentTransaction;
import com.payment_service.repository.PaymentTransactionRepository;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final PaymentTransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public Page<PaymentTransactionResponse> findTransactionsByUserId(String userId, Pageable pageable) {
        Page<PaymentTransaction> transactionsPage = transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        // Map the entity page to a DTO page
        return transactionsPage.map(this::convertToDto);
    }

    private PaymentTransactionResponse convertToDto(PaymentTransaction transaction) {
        return new PaymentTransactionResponse(
            transaction.getId(),
            transaction.getAmountPaid(),
            transaction.getCurrency(),
            transaction.getStatus(),
            transaction.getCreatedAt(),
            transaction.getPaymentIntentId()
        );
    }
}
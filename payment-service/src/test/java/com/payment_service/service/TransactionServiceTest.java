package com.payment_service.service;

import com.payment_service.dto.PaymentTransactionResponse;
import com.payment_service.model.PaymentTransaction;
import com.payment_service.repository.PaymentTransactionRepository;
import com.shortscreator.shared.enums.TransactionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private PaymentTransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void givenUserHasTransactions_whenFindTransactionsByUserId_thenReturnsMappedDtoPage() {
        String userId = "user-123";
        Pageable pageable = PageRequest.of(0, 10);

        PaymentTransaction tx1 = new PaymentTransaction();
        tx1.setUserId(userId);
        tx1.setAmountPaid(5000);
        tx1.setCurrency("USD");
        tx1.setStatus(TransactionStatus.COMPLETED);
        tx1.setCreatedAt(Instant.now());
        tx1.setPaymentIntentId("pi_1");

        PaymentTransaction tx2 = new PaymentTransaction();
        tx2.setUserId(userId);
        tx2.setAmountPaid(1000);
        tx2.setCurrency("USD");
        tx2.setStatus(TransactionStatus.PENDING);
        tx2.setCreatedAt(Instant.now().minusSeconds(3600));
        tx2.setPaymentIntentId("pi_2");

        Page<PaymentTransaction> transactionPage = new PageImpl<>(List.of(tx1, tx2), pageable, 2);

        when(transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)).thenReturn(transactionPage);

        Page<PaymentTransactionResponse> resultPage = transactionService.findTransactionsByUserId(userId, pageable);

        assertThat(resultPage.getTotalElements()).isEqualTo(2);
        assertThat(resultPage.getContent()).hasSize(2);

        PaymentTransactionResponse response1 = resultPage.getContent().get(0);
        assertThat(response1.amountPaid()).isEqualTo(tx1.getAmountPaid());
        assertThat(response1.status()).isEqualTo(tx1.getStatus());
        assertThat(response1.paymentIntentId()).isEqualTo(tx1.getPaymentIntentId());
    }
}
package com.payment_service.service;

import com.payment_service.exception.InsufficientFundsException;
import com.payment_service.exception.ResourceNotFoundException;
import com.payment_service.model.UserBalance;
import com.payment_service.repository.UserBalanceRepository;
import com.shortscreator.shared.dto.ChargeReasonV1;
import com.shortscreator.shared.dto.DebitRequestV1;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

    @Mock
    private UserBalanceRepository userBalanceRepository;

    @InjectMocks
    private BalanceService balanceService;

    // region getBalanceByUserId Tests (No changes needed here)
    @Test
    void givenUserExists_whenGetBalanceByUserId_thenReturnsExistingBalance() {
        String userId = "user-123";
        UserBalance existingBalance = new UserBalance(userId);
        existingBalance.setBalanceInCents(5000L);
        when(userBalanceRepository.findByUserId(userId)).thenReturn(Optional.of(existingBalance));
        UserBalance result = balanceService.getBalanceByUserId(userId);
        assertThat(result).isEqualTo(existingBalance);
        verify(userBalanceRepository, never()).save(any(UserBalance.class));
    }

    @Test
    void givenUserDoesNotExist_whenGetBalanceByUserId_thenCreatesAndReturnsNewBalance() {
        String userId = "new-user-456";
        UserBalance newBalance = new UserBalance(userId);
        when(userBalanceRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userBalanceRepository.save(any(UserBalance.class))).thenReturn(newBalance);
        UserBalance result = balanceService.getBalanceByUserId(userId);
        assertThat(result).isEqualTo(newBalance);
        verify(userBalanceRepository).save(any(UserBalance.class));
    }

    @Test
    void givenRaceConditionOnCreate_whenGetBalanceByUserId_thenHandlesConflictAndReFetches() {
        String userId = "concurrent-user-789";
        UserBalance concurrentlyCreatedBalance = new UserBalance(userId);
        when(userBalanceRepository.findByUserId(userId))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(concurrentlyCreatedBalance));
        when(userBalanceRepository.save(any(UserBalance.class)))
                .thenThrow(new DataIntegrityViolationException("Simulated unique constraint violation"));
        UserBalance result = balanceService.getBalanceByUserId(userId);
        assertThat(result).isEqualTo(concurrentlyCreatedBalance);
    }
    // endregion

    // region debitUserBalance Tests
    @Test
    void givenSufficientFunds_whenDebitUserBalance_thenSucceeds() {
        String userId = "user-123";
        long initialBalance = 1000L;
        String idempotencyKey = UUID.randomUUID().toString();
        Integer price = 400;
        DebitRequestV1 request = new DebitRequestV1(userId, price, ChargeReasonV1.AI_TEXT_GENERATION, idempotencyKey, null);

        UserBalance userBalance = new UserBalance(userId);
        userBalance.setBalanceInCents(initialBalance);

        when(userBalanceRepository.findByUserId(userId)).thenReturn(Optional.of(userBalance));

        balanceService.debitUserBalance(request);

        assertThat(userBalance.getBalanceInCents()).isEqualTo(initialBalance - price);
        verify(userBalanceRepository).save(userBalance);
    }

    @Test
    void givenUserNotFound_whenDebitUserBalance_thenThrowsResourceNotFoundException() {
        String userId = "non-existent-user";
        Integer price = 100;
        String idempotencyKey = UUID.randomUUID().toString();
        DebitRequestV1 request = new DebitRequestV1(userId, price, ChargeReasonV1.AI_TEXT_GENERATION, idempotencyKey, null);

        when(userBalanceRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> balanceService.debitUserBalance(request));
        verify(userBalanceRepository, never()).save(any(UserBalance.class));
    }

    @Test
    void givenCurrencyMismatch_whenDebitUserBalance_thenThrowsIllegalArgumentException() {
        String userId = "user-123";
        Integer price = 100;
        String idempotencyKey = UUID.randomUUID().toString();
        DebitRequestV1 request = new DebitRequestV1(userId, price, ChargeReasonV1.AI_TEXT_GENERATION, idempotencyKey, null);

        UserBalance userBalance = new UserBalance(userId);
        userBalance.setCurrency("USD"); // Balance in USD

        when(userBalanceRepository.findByUserId(userId)).thenReturn(Optional.of(userBalance));

        assertThrows(IllegalArgumentException.class, () -> balanceService.debitUserBalance(request));
        verify(userBalanceRepository, never()).save(any(UserBalance.class));
    }

    @Test
    void givenInsufficientFunds_whenDebitUserBalance_thenThrowsInsufficientFundsException() {
        String userId = "user-123";
        Integer price = 5000; // Requesting 5000 cents
        String idempotencyKey = UUID.randomUUID().toString();
        DebitRequestV1 request = new DebitRequestV1(userId, price, ChargeReasonV1.AI_TEXT_GENERATION, idempotencyKey, null);
        
        UserBalance userBalance = new UserBalance(userId);
        userBalance.setBalanceInCents(4999L);

        when(userBalanceRepository.findByUserId(userId)).thenReturn(Optional.of(userBalance));

        assertThrows(InsufficientFundsException.class, () -> balanceService.debitUserBalance(request));
        verify(userBalanceRepository, never()).save(any(UserBalance.class));
    }
    // endregion
}
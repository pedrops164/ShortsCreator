package com.payment_service.service;

import com.payment_service.dto.DebitRequest;
import com.payment_service.exception.InsufficientFundsException;
import com.payment_service.exception.ResourceNotFoundException;
import com.payment_service.model.UserBalance;
import com.payment_service.repository.UserBalanceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

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

    // region getBalanceByUserId Tests
    @Test
    void givenUserExists_whenGetBalanceByUserId_thenReturnsExistingBalance() {
        String userId = "user-123";
        UserBalance existingBalance = new UserBalance(userId);
        existingBalance.setBalanceInCents(5000L);

        when(userBalanceRepository.findByUserId(userId)).thenReturn(Optional.of(existingBalance));

        UserBalance result = balanceService.getBalanceByUserId(userId);

        assertThat(result).isEqualTo(existingBalance);
        assertThat(result.getBalanceInCents()).isEqualTo(5000L);
        verify(userBalanceRepository, never()).save(any(UserBalance.class));
    }

    @Test
    void givenUserDoesNotExist_whenGetBalanceByUserId_thenCreatesAndReturnsNewBalance() {
        String userId = "new-user-456";
        UserBalance newBalance = new UserBalance(userId); // Initial balance is 0

        when(userBalanceRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userBalanceRepository.save(any(UserBalance.class))).thenReturn(newBalance);

        UserBalance result = balanceService.getBalanceByUserId(userId);

        assertThat(result).isEqualTo(newBalance);
        assertThat(result.getBalanceInCents()).isZero();
        verify(userBalanceRepository).save(any(UserBalance.class));
    }

    @Test
    void givenRaceConditionOnCreate_whenGetBalanceByUserId_thenHandlesConflictAndReFetches() {
        String userId = "concurrent-user-789";
        UserBalance concurrentlyCreatedBalance = new UserBalance(userId);

        // Simulate the race condition flow
        when(userBalanceRepository.findByUserId(userId))
                .thenReturn(Optional.empty()) // First call: doesn't exist
                .thenReturn(Optional.of(concurrentlyCreatedBalance)); // Second call: now it exists

        // Simulate that our 'save' fails because another thread just saved it
        when(userBalanceRepository.save(any(UserBalance.class)))
                .thenThrow(new DataIntegrityViolationException("Simulated unique constraint violation"));

        UserBalance result = balanceService.getBalanceByUserId(userId);

        assertThat(result).isEqualTo(concurrentlyCreatedBalance);
        verify(userBalanceRepository, times(2)).findByUserId(userId);
        verify(userBalanceRepository, times(1)).save(any(UserBalance.class));
    }
    // endregion

    // region debitUserBalance Tests
    @Test
    void givenSufficientFunds_whenDebitUserBalance_thenSucceeds() {
        String userId = "user-123";
        long initialBalance = 1000L;
        long amountToDebit = 400L;
        DebitRequest request = new DebitRequest(userId, amountToDebit, "USD");
        UserBalance userBalance = new UserBalance(userId);
        userBalance.setBalanceInCents(initialBalance);

        when(userBalanceRepository.findByUserId(userId)).thenReturn(Optional.of(userBalance));

        balanceService.debitUserBalance(request);

        assertThat(userBalance.getBalanceInCents()).isEqualTo(initialBalance - amountToDebit);
        verify(userBalanceRepository).save(userBalance);
    }

    @Test
    void givenUserNotFound_whenDebitUserBalance_thenThrowsResourceNotFoundException() {
        String userId = "non-existent-user";
        DebitRequest request = new DebitRequest(userId, 100L, "USD");

        when(userBalanceRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> balanceService.debitUserBalance(request));
        verify(userBalanceRepository, never()).save(any(UserBalance.class));
    }

    @Test
    void givenCurrencyMismatch_whenDebitUserBalance_thenThrowsIllegalArgumentException() {
        String userId = "user-123";
        DebitRequest request = new DebitRequest(userId, 100L, "EUR"); // Request in EUR
        UserBalance userBalance = new UserBalance(userId);
        userBalance.setCurrency("USD"); // Balance in USD

        when(userBalanceRepository.findByUserId(userId)).thenReturn(Optional.of(userBalance));

        assertThrows(IllegalArgumentException.class, () -> balanceService.debitUserBalance(request));
        verify(userBalanceRepository, never()).save(any(UserBalance.class));
    }

    @Test
    void givenInsufficientFunds_whenDebitUserBalance_thenThrowsInsufficientFundsException() {
        String userId = "user-123";
        DebitRequest request = new DebitRequest(userId, 5000L, "USD");
        UserBalance userBalance = new UserBalance(userId);
        userBalance.setBalanceInCents(4999L);

        when(userBalanceRepository.findByUserId(userId)).thenReturn(Optional.of(userBalance));

        assertThrows(InsufficientFundsException.class, () -> balanceService.debitUserBalance(request));
        verify(userBalanceRepository, never()).save(any(UserBalance.class));
    }
    // endregion
}
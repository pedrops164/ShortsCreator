package com.payment_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payment_service.dto.DebitRequest;
import com.payment_service.model.UserBalance;
import com.payment_service.repository.UserBalanceRepository;
import com.payment_service.exception.InsufficientFundsException;
import com.payment_service.exception.ResourceNotFoundException;

@Slf4j
@Service
public class BalanceService {

    private final UserBalanceRepository userBalanceRepository;

    public BalanceService(UserBalanceRepository userBalanceRepository) {
        this.userBalanceRepository = userBalanceRepository;
    }

    /**
     * Retrieves the balance for a specific user. If the user has no balance record,
     * it creates one with a zero balance in the default currency (USD).
     *
     * @param userId The unique ID of the user.
     * @return The UserBalance entity.
     */
    public UserBalance getBalanceByUserId(String userId) {
        log.info("Fetching balance for user ID: {}", userId);
        // Find the balance or create a new one with 0 balance in USD if it doesn't exist.
        return userBalanceRepository.findByUserId(userId)
            .orElseGet(() -> {
                log.info("No balance record found for user ID: {}. Creating a new one.", userId);
                UserBalance newBalance = new UserBalance(userId);
                return userBalanceRepository.save(newBalance);
            });
    }

    /**
     * Debits a specified amount from a user's balance. This operation is transactional,
     * ensuring atomicity. It checks for sufficient funds and currency consistency.
     *
     * @param debitRequest The request DTO containing user ID, amount, and currency.
     * @throws ResourceNotFoundException  if the user's balance record doesn't exist.
     * @throws InsufficientFundsException if the user's balance is less than the debit amount.
     * @throws IllegalArgumentException if the debit currency does not match the user's balance currency.
     */
    @Transactional
    public void debitUserBalance(DebitRequest debitRequest) {
        String userId = debitRequest.userId();
        long amountToDebit = debitRequest.amount();

        log.info("Attempting to debit {} {} from user ID: {}", amountToDebit, debitRequest.currency(), userId);

        // This read operation is locked by the transaction until it commits or rolls back.
        UserBalance userBalance = userBalanceRepository.findByUserId(userId)
            .orElseThrow(() -> {
                log.error("Debit failed: No balance record found for user ID: {}", userId);
                return new ResourceNotFoundException("User balance for user ID " + userId + " not found.");
            });

        // Validate Currency
        if (!userBalance.getCurrency().equals(debitRequest.currency())) {
            log.error("Debit failed for user {}: Currency mismatch. Account is in {} but debit was for {}",
                userId, userBalance.getCurrency(), debitRequest.currency());
            throw new IllegalArgumentException("Debit currency does not match account currency.");
        }

        // Validate Funds
        if (userBalance.getBalanceInCents() < amountToDebit) {
            log.warn("Debit failed for user {}: Insufficient funds. Current balance: {}, required: {}",
                userId, userBalance.getBalanceInCents(), amountToDebit);
            throw new InsufficientFundsException("Insufficient funds for this operation.");
        }

        // Perform Debit
        long newBalance = userBalance.getBalanceInCents() - amountToDebit;
        userBalance.setBalanceInCents(newBalance);
        userBalanceRepository.save(userBalance); // The transaction commit will persist this change.

        log.info("Successfully debited {} from user {}. New balance: {}", amountToDebit, userId, newBalance);
    }
}
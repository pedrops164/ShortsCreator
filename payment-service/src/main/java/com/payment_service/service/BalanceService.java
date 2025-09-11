package com.payment_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payment_service.model.GenerationCharge;
import com.payment_service.model.UserBalance;
import com.payment_service.repository.GenerationChargeRepository;
import com.payment_service.repository.UserBalanceRepository;
import com.shortscreator.shared.dto.DebitRequestV1;
import com.shortscreator.shared.enums.ChargeStatus;
import com.shortscreator.shared.enums.ContentType;
import com.payment_service.exception.InsufficientFundsException;
import com.payment_service.exception.ResourceNotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceService {

    private final UserBalanceRepository userBalanceRepository;
    private final GenerationChargeRepository generationChargeRepository;

    private static final long WELCOME_CREDIT_IN_CENTS = 100L; // $1.00

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
                // If it doesn't exist, attempt to create and save it.
                log.info("No balance record found for user ID: {}. Attempting to create a new one.", userId);
                UserBalance newBalance = new UserBalance(userId);
                try {
                    return userBalanceRepository.save(newBalance);
                } catch (DataIntegrityViolationException e) {
                    // If we get this specific exception, it means a concurrent request beat us to it.
                    log.warn("Race condition detected for user ID: {}. Another request created the balance. Re-fetching.", userId);
                    // The record is guaranteed to exist now, so we can fetch it again.
                    // The .get() is safe here because we know the record was just created.
                    return userBalanceRepository.findByUserId(userId).get();
                }
            });
    }

    /**
     * Debits a specified amount from a user's balance. This operation is transactional,
     * ensuring atomicity. It checks for sufficient funds and currency consistency.
     *
     * @param debitRequest The request DTO containing user ID, amount, and currency.
     * @throws ResourceNotFoundException (404 error)  if the user's balance record doesn't exist.
     * @throws InsufficientFundsException (409 error) if the user's balance is less than the debit amount.
     * @throws IllegalArgumentException if the debit currency does not match the user's balance currency.
     */
    @Transactional
    public void debitUserBalance(DebitRequestV1 debitRequest) {
        String userId = debitRequest.userId();
        Integer amountToDebit = debitRequest.amountInCents();
        String currency = "USD"; // USD by default

        log.info("Attempting to debit {} {} from user ID: {} with metadata: {}", amountToDebit, currency, userId, debitRequest.metadata());
        // This read operation is locked by the transaction until it commits or rolls back.
        UserBalance userBalance = userBalanceRepository.findByUserId(userId)
            .orElseThrow(() -> {
                log.error("Debit failed: No balance record found for user ID: {}", userId);
                return new ResourceNotFoundException("User balance for user ID " + userId + " not found.");
            });

        // Validate Currency
        if (!userBalance.getCurrency().equals(currency)) {
            log.error("Debit failed for user {}: Currency mismatch. Account is in {} but debit was for {}",
                userId, userBalance.getCurrency(), currency);
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

        if (debitRequest.metadata() != null && debitRequest.metadata().containsKey("contentId") && debitRequest.metadata().get("ContentType") != null) {
            String contentId = debitRequest.metadata().get("contentId");
            ContentType contentType = ContentType.valueOf(debitRequest.metadata().get("ContentType"));
            // Create and save the generation charge record
            GenerationCharge charge = GenerationCharge.builder()
                    .userId(debitRequest.userId())
                    .contentId(contentId)
                    .contentType(contentType)
                    .amount(amountToDebit)
                    .currency(currency)
                    .status(ChargeStatus.COMPLETED)
                    .build();
            generationChargeRepository.save(charge);
            log.info("Successfully debited user {} and recorded charge for content {}", userId, contentId);
        }

        // For now, only content generation charges are recorded.
    }

    /**
     * Refunds a generation charge by content ID. This operation credits the user's balance
     * and updates the charge status to REFUNDED.
     *
     * @param contentId The unique ID of the content for which the charge is being refunded.
     * @throws ResourceNotFoundException if the GenerationCharge or UserBalance is not found.
     */
    @Transactional
    public void refundGenerationCharge(String contentId) {
        log.info("Attempting to refund charge for contentId: {}", contentId);

        // Find the original charge.
        GenerationCharge charge = generationChargeRepository.findByContentId(contentId)
                .orElseThrow(() -> {
                    log.error("Refund failed for contentId {}: GenerationCharge not found.", contentId);
                    return new ResourceNotFoundException("GenerationCharge not found for contentId: " + contentId);
                });

        // IDEMPOTENCY CHECK: If it's already refunded, do nothing and succeed.
        if (charge.getStatus() == ChargeStatus.REFUNDED) {
            log.warn("Charge for contentId {} has already been refunded. Ignoring request.", contentId);
            return;
        }

        // Find the user's balance to credit the amount back.
        UserBalance userBalance = userBalanceRepository.findByUserId(charge.getUserId())
                .orElseThrow(() -> {
                    log.error("Refund failed for contentId {}: UserBalance not found.", contentId);
                    return new ResourceNotFoundException("UserBalance not found for user: " + charge.getUserId());
                });

        // Perform the refund (credit the user's balance).
        long newBalance = userBalance.getBalanceInCents() + charge.getAmount();
        userBalance.setBalanceInCents(newBalance);
        userBalanceRepository.save(userBalance);

        // Update the charge status to REFUNDED.
        charge.setStatus(ChargeStatus.REFUNDED);
        generationChargeRepository.save(charge);

        log.info("Successfully refunded {} {} to user {} for failed contentId {}. New balance: {}",
                charge.getAmount(), charge.getCurrency(), charge.getUserId(), contentId, newBalance);
    }

    @Transactional
    public void createNewUserBalanceWithWelcomeCredit(String userId) {
        // IDEMPOTENCY CHECK: Do not create a balance if one already exists.
        if (userBalanceRepository.findByUserId(userId).isPresent()) {
            log.warn("Attempted to create a balance for existing user ID: {}. Ignoring event.", userId);
            return;
        }

        log.info("Creating new balance for user ID: {} with a welcome credit of {} cents.", userId, WELCOME_CREDIT_IN_CENTS);
        UserBalance newBalance = new UserBalance(userId);
        newBalance.setBalanceInCents(WELCOME_CREDIT_IN_CENTS);
        
        userBalanceRepository.save(newBalance);
        log.info("Successfully created balance for user ID: {}", userId);
    }
}
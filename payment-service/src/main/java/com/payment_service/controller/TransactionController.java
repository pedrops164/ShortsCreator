package com.payment_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.payment_service.dto.UnifiedTransactionView;
import com.payment_service.service.TransactionService;
import com.shortscreator.shared.dto.CustomPage;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<CustomPage<UnifiedTransactionView>> getTransactions(
            @RequestHeader("X-User-ID") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        
        Pageable pageable = PageRequest.of(page, size);

        // Get the standard Page object from the service
        Page<UnifiedTransactionView> transactions = transactionService.findUnifiedTransactionsByUserId(userId, pageable);
        // Convert to CustomPage
        CustomPage<UnifiedTransactionView> customPage = new CustomPage<>(
                transactions.getContent(),
                transactions.getNumber(),
                transactions.getTotalPages(),
                transactions.isFirst(),
                transactions.isLast()
        );

        // Return the custom page response
        return ResponseEntity.ok(customPage);
    }
}
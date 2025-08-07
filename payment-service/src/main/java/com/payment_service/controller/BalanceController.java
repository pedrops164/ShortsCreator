package com.payment_service.controller;

import com.payment_service.dto.BalanceResponse;
import com.payment_service.model.UserBalance;
import com.payment_service.service.BalanceService;
import com.shortscreator.shared.dto.DebitRequestV1;
import com.shortscreator.shared.dto.RefundRequestV1;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/balance")
public class BalanceController {

    private final BalanceService balanceService;

    public BalanceController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    /**
     * Public-facing endpoint to fetch the current user's balance.
     * The X-User-ID header is injected by the API Gateway after authentication.
     */
    @GetMapping
    public ResponseEntity<BalanceResponse> getUserBalance(@RequestHeader("X-User-ID") String userId) {
        // Delegate finding the user's balance to the service layer.
        UserBalance userBalance = balanceService.getBalanceByUserId(userId);
        
        // Create the response DTO from the entity.
        BalanceResponse response = new BalanceResponse(
            userBalance.getBalanceInCents(),
            userBalance.getCurrency()
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Internal-facing endpoint for other services (like CGS) to debit a user's balance.
     * This endpoint must NOT be exposed by the API Gateway.
     */
    @PostMapping("/debit")
    public ResponseEntity<Void> debitBalance(@RequestBody DebitRequestV1 debitRequest) {
        // Delegate the entire debit operation to the service layer.
        balanceService.debitUserBalance(debitRequest);
        
        // A 200 OK response indicates the debit was successful.
        // The service will throw an exception (handled by a @ControllerAdvice) if funds are insufficient.
        return ResponseEntity.ok().build();
    }

    /**
     * Internal-facing endpoint for refunding a generation charge.
     * This endpoint must NOT be exposed by the API Gateway.
     */
    @PostMapping("/refund")
    public ResponseEntity<Void> refundCharge(@RequestBody RefundRequestV1 refundRequest) {
        balanceService.refundGenerationCharge(refundRequest.contentId());
        return ResponseEntity.ok().build();
    }
}

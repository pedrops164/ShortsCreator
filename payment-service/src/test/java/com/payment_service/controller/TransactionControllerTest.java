package com.payment_service.controller;

import com.payment_service.dto.UnifiedTransactionView;
import com.payment_service.enums.TransactionType;
import com.payment_service.service.TransactionService;
import com.shortscreator.shared.enums.TransactionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    private static final String USER_ID = "user-123";

    @Test
    void whenGetTransactions_thenReturnsPageOfTransactions() throws Exception {
        // Given
        UUID transactionId = UUID.randomUUID();
        UnifiedTransactionView txResponse = new UnifiedTransactionView(
            transactionId, TransactionType.DEPOSIT, "Balance Top-up", 5000, "USD", TransactionStatus.COMPLETED, Instant.now()
        );
        Page<UnifiedTransactionView> transactionPage = new PageImpl<>(
            List.of(txResponse), PageRequest.of(0, 5), 1
        );

        when(transactionService.findUnifiedTransactionsByUserId(eq(USER_ID), any(PageRequest.class)))
            .thenReturn(transactionPage);

        // When & Then
        mockMvc.perform(get("/api/v1/transactions")
                .header("X-User-ID", USER_ID)
                .param("page", "0")
                .param("size", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].id").value(transactionId.toString()))
            .andExpect(jsonPath("$.content[0].amount").value(5000));
    }
}
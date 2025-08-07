package com.payment_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment_service.exception.InsufficientFundsException;
import com.payment_service.model.UserBalance;
import com.payment_service.service.BalanceService;
import com.shortscreator.shared.dto.ContentPriceV1;
import com.shortscreator.shared.dto.DebitRequestV1;
import com.shortscreator.shared.enums.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BalanceController.class)
class BalanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BalanceService balanceService;

    private static final String USER_ID = "user-123";

    @Test
    void givenValidUserId_whenGetUserBalance_thenReturnsOkWithBalance() throws Exception {
        // Given
        UserBalance userBalance = new UserBalance(USER_ID);
        userBalance.setBalanceInCents(5000L);
        userBalance.setCurrency("USD");
        when(balanceService.getBalanceByUserId(USER_ID)).thenReturn(userBalance);

        // When & Then
        mockMvc.perform(get("/api/v1/balance")
                .header("X-User-ID", USER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.balanceInCents").value(5000))
            .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void givenValidDebitRequest_whenDebitBalance_thenReturnsOk() throws Exception {
        // Given
        ContentPriceV1 priceDetails = new ContentPriceV1(1000, "USD");
        DebitRequestV1 debitRequest = new DebitRequestV1(USER_ID, "content-1", priceDetails, ContentType.REDDIT_STORY);
        
        doNothing().when(balanceService).debitUserBalance(any(DebitRequestV1.class));

        // When & Then
        mockMvc.perform(post("/api/v1/balance/debit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(debitRequest)))
            .andExpect(status().isOk());
    }
    
    @Test
    void givenInsufficientFunds_whenDebitBalance_thenReturnsError() throws Exception {
        // Given
        ContentPriceV1 priceDetails = new ContentPriceV1(1000, "USD");
        DebitRequestV1 debitRequest = new DebitRequestV1(USER_ID, "content-1", priceDetails, ContentType.REDDIT_STORY);
        
        doThrow(new InsufficientFundsException("Not enough money"))
            .when(balanceService).debitUserBalance(any(DebitRequestV1.class));

        // When & Then
        mockMvc.perform(post("/api/v1/balance/debit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(debitRequest)))
            .andExpect(status().isConflict()); // Assuming @ControllerAdvice maps this to 409 Conflict
    }
}
package com.payment_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment_service.dto.CreateCheckoutRequest;
import com.payment_service.service.PaymentService;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    private static final String USER_ID = "user-123";

    @Test
    void givenValidRequest_whenCreateCheckoutSession_thenReturnsOkWithUrl() throws Exception {
        // Given
        CreateCheckoutRequest request = new CreateCheckoutRequest("pro-pack");
        Session mockSession = mock(Session.class);
        when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/session_123");
        
        when(paymentService.createStripeCheckoutSession(any(CreateCheckoutRequest.class), eq(USER_ID)))
            .thenReturn(mockSession);
        
        // When & Then
        mockMvc.perform(post("/api/v1/payments/create-checkout-session")
                .header("X-User-ID", USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.redirectUrl").value("https://checkout.stripe.com/session_123"));
    }
    
    @Test
    void givenInvalidRequest_whenCreateCheckoutSession_thenReturnsBadRequest() throws Exception {
        // Given a request with a null packageId, which should fail @Valid
        CreateCheckoutRequest request = new CreateCheckoutRequest(null);
        
        // When & Then
        mockMvc.perform(post("/api/v1/payments/create-checkout-session")
                .header("X-User-ID", USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}
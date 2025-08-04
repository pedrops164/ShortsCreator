package com.payment_service.controller;

import com.payment_service.config.StripeProperties;
import com.payment_service.service.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StripeController.class)
class StripeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private StripeProperties stripeProperties;

    private MockedStatic<Webhook> mockedWebhook;

    @BeforeEach
    void setUp() {
        // Mock the static Webhook.constructEvent method
        mockedWebhook = Mockito.mockStatic(Webhook.class);
        // Provide a default value for the webhook secret
        when(stripeProperties.webhookSecret()).thenReturn("whsec_test_secret");
    }

    @AfterEach
    void tearDown() {
        // Close the static mock to prevent test pollution
        mockedWebhook.close();
    }

    @Test
    void givenInvalidSignature_whenHandleStripeWebhook_thenReturnsBadRequest() throws Exception {
        // Given
        mockedWebhook.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
            .thenThrow(new SignatureVerificationException("Invalid signature", "sig_123"));

        // When & Then
        mockMvc.perform(post("/api/v1/stripe/webhooks")
                .header("Stripe-Signature", "invalid_signature")
                .content("{\"type\": \"checkout.session.completed\"}"))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void givenValidSignatureAndCheckoutCompletedEvent_whenHandleWebhook_thenCallsServiceAndReturnsOk() throws Exception {
        // Given
        String payload = "{\"type\": \"checkout.session.completed\"}";
        Session session = new Session(); // A mock or empty Session object
        
        // Mock the deserialization process
        Event mockEvent = mock(Event.class);
        EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);
        when(mockEvent.getType()).thenReturn("checkout.session.completed");
        when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
        when(mockDeserializer.getObject()).thenReturn(Optional.of(session));

        mockedWebhook.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
            .thenReturn(mockEvent);

        // When & Then
        mockMvc.perform(post("/api/v1/stripe/webhooks")
                .header("Stripe-Signature", "valid_signature")
                .content(payload))
            .andExpect(status().isOk());
            
        // Verify the correct service method was called
        verify(paymentService).handleCheckoutSessionCompleted(session);
    }
    
    @Test
    void givenValidSignatureAndUnhandledEvent_whenHandleWebhook_thenReturnsOk() throws Exception {
        // Given
        String payload = "{\"type\": \"customer.created\"}"; // An event we don't handle
        Event mockEvent = mock(Event.class);
        EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);
        when(mockEvent.getType()).thenReturn("customer.created");
        when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
        when(mockDeserializer.getObject()).thenReturn(Optional.of(mock(Session.class))); // Object type doesn't matter here

        mockedWebhook.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
            .thenReturn(mockEvent);

        // When & Then
        mockMvc.perform(post("/api/v1/stripe/webhooks")
                .header("Stripe-Signature", "valid_signature")
                .content(payload))
            .andExpect(status().isOk());
            
        // Verify no payment service methods were called
        verify(paymentService, never()).handleCheckoutSessionCompleted(any());
        verify(paymentService, never()).handleAsyncPaymentSucceeded(any());
    }
}
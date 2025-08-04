package com.payment_service.service;

import com.payment_service.config.StripeProperties;
import com.payment_service.dto.CreateCheckoutRequest;
import com.payment_service.messaging.PaymentStatusDispatcher;
import com.payment_service.model.PaymentTransaction;
import com.payment_service.model.UserBalance;
import com.payment_service.repository.PaymentTransactionRepository;
import com.payment_service.repository.UserBalanceRepository;
import com.shortscreator.shared.dto.PaymentStatusUpdateV1;
import com.shortscreator.shared.enums.TransactionStatus;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private StripeProperties stripeProperties;
    @Mock private UserBalanceRepository userBalanceRepository;
    @Mock private PaymentTransactionRepository paymentTransactionRepository;
    @Mock private PaymentStatusDispatcher paymentStatusDispatcher;
    
    @InjectMocks private PaymentService paymentService;

    private MockedStatic<Session> mockedSession;

    @BeforeEach
    void setUp() {
        // Mock the static `Session.create` method before each test
        mockedSession = Mockito.mockStatic(Session.class);
    }

    @AfterEach
    void tearDown() {
        // Close the static mock after each test to avoid test pollution
        mockedSession.close();
    }

    // region createStripeCheckoutSession Tests
    @Test
    void givenValidPackage_whenCreateStripeCheckoutSession_thenReturnsSession() throws StripeException {
        String userId = "user-123";
        String packageId = "pro-pack";
        String priceId = "price_123xyz";
        CreateCheckoutRequest request = new CreateCheckoutRequest(packageId);
        Session mockSession = new Session();
        mockSession.setId("cs_test_123");

        when(stripeProperties.priceMap()).thenReturn(Map.of(packageId, priceId));
        when(stripeProperties.successUrl()).thenReturn("http://test.com/success");
        when(stripeProperties.cancelUrl()).thenReturn("http://test.com/cancel");
        
        mockedSession.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(mockSession);

        Session result = paymentService.createStripeCheckoutSession(request, userId);

        assertThat(result.getId()).isEqualTo("cs_test_123");
        
        // Verify that the metadata contains our internal user ID
        ArgumentCaptor<SessionCreateParams> paramsCaptor = ArgumentCaptor.forClass(SessionCreateParams.class);
        mockedSession.verify(() -> Session.create(paramsCaptor.capture()));
        assertThat(paramsCaptor.getValue().getMetadata()).containsEntry("userId", userId);
    }

    @Test
    void givenInvalidPackage_whenCreateStripeCheckoutSession_thenThrowsException() {
        String userId = "user-123";
        CreateCheckoutRequest request = new CreateCheckoutRequest("invalid-pack");

        when(stripeProperties.priceMap()).thenReturn(Map.of("pro-pack", "price_123xyz"));
        
        assertThrows(IllegalArgumentException.class, () -> paymentService.createStripeCheckoutSession(request, userId));
    }
    
    @Test
    void givenStripeError_whenCreateStripeCheckoutSession_thenThrowsRuntimeException() throws StripeException {
        String userId = "user-123";
        CreateCheckoutRequest request = new CreateCheckoutRequest("pro-pack");
        
        when(stripeProperties.priceMap()).thenReturn(Map.of("pro-pack", "price_123xyz"));
        mockedSession.when(() -> Session.create(any(SessionCreateParams.class))).thenThrow(mock(StripeException.class));

        assertThrows(RuntimeException.class, () -> paymentService.createStripeCheckoutSession(request, userId));
    }
    // endregion

    // region Webhook Handler Tests
    @Test
    void givenSessionAlreadyProcessed_whenHandleCheckoutCompleted_thenSkips() {
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("cs_test_123");
        when(paymentTransactionRepository.existsByStripeCheckoutSessionId("cs_test_123")).thenReturn(true);
        
        paymentService.handleCheckoutSessionCompleted(session);
        
        verify(userBalanceRepository, never()).save(any());
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    void givenSyncPaymentForNewUser_whenHandleCheckoutCompleted_thenCreatesBalanceAndTransaction() {
        String userId = "new-user-456";
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("cs_test_123");
        when(session.getMetadata()).thenReturn(Map.of("userId", userId));
        when(session.getCurrency()).thenReturn("usd");
        when(session.getAmountTotal()).thenReturn(5000L);
        when(session.getPaymentIntent()).thenReturn("pi_123");
        when(session.getPaymentStatus()).thenReturn("paid");

        when(paymentTransactionRepository.existsByStripeCheckoutSessionId(any())).thenReturn(false);
        when(userBalanceRepository.findByUserId(userId)).thenReturn(Optional.empty());

        paymentService.handleCheckoutSessionCompleted(session);

        ArgumentCaptor<UserBalance> balanceCaptor = ArgumentCaptor.forClass(UserBalance.class);
        verify(userBalanceRepository).save(balanceCaptor.capture());
        assertThat(balanceCaptor.getValue().getUserId()).isEqualTo(userId);
        assertThat(balanceCaptor.getValue().getBalanceInCents()).isEqualTo(5000L);

        ArgumentCaptor<PaymentTransaction> transactionCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getValue().getStatus()).isEqualTo(TransactionStatus.COMPLETED);
    }
    
    @Test
    void givenAsyncPaymentForExistingUser_whenHandleCheckoutCompleted_thenCreatesPendingTransaction() {
        String userId = "user-123";
        Session session = mock(Session.class);
        UserBalance existingBalance = new UserBalance(userId);
        existingBalance.setBalanceInCents(1000L);

        when(session.getId()).thenReturn("cs_test_123");
        when(session.getMetadata()).thenReturn(Map.of("userId", userId));
        when(session.getCurrency()).thenReturn("usd");
        when(session.getAmountTotal()).thenReturn(5000L);
        when(session.getPaymentIntent()).thenReturn("pi_123");
        when(session.getPaymentStatus()).thenReturn("unpaid"); // Async payment status

        when(paymentTransactionRepository.existsByStripeCheckoutSessionId(any())).thenReturn(false);
        when(userBalanceRepository.findByUserId(userId)).thenReturn(Optional.of(existingBalance));

        paymentService.handleCheckoutSessionCompleted(session);

        // Balance should NOT be updated yet for pending transactions
        verify(userBalanceRepository, never()).save(any());
        
        ArgumentCaptor<PaymentTransaction> transactionCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getValue().getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(transactionCaptor.getValue().getUserId()).isEqualTo(userId);
    }
    
    @Test
    void givenPendingTransaction_whenHandleAsyncPaymentSucceeded_thenUpdatesBalanceAndNotifies() {
        String userId = "user-123";
        long amount = 5000L;
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("cs_test_123");
        when(session.getAmountTotal()).thenReturn(amount);

        PaymentTransaction pendingTransaction = new PaymentTransaction();
        pendingTransaction.setUserId(userId);
        pendingTransaction.setStatus(TransactionStatus.PENDING);
        
        UserBalance userBalance = new UserBalance(userId);
        userBalance.setBalanceInCents(1000L);
        
        when(paymentTransactionRepository.findByStripeCheckoutSessionId("cs_test_123")).thenReturn(Optional.of(pendingTransaction));
        when(userBalanceRepository.findByUserId(userId)).thenReturn(Optional.of(userBalance));
        
        paymentService.handleAsyncPaymentSucceeded(session);

        assertThat(pendingTransaction.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(userBalance.getBalanceInCents()).isEqualTo(1000L + amount);
        verify(paymentTransactionRepository).save(pendingTransaction);
        verify(userBalanceRepository).save(userBalance);
        verify(paymentStatusDispatcher).dispatchPaymentStatus(any(PaymentStatusUpdateV1.class));
    }
    
    @Test
    void givenPendingTransaction_whenHandleAsyncPaymentFailed_thenUpdatesStatusAndNotifies() {
        String userId = "user-123";
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("cs_test_123");
        when(session.getMetadata()).thenReturn(Map.of("userId", userId));
        when(session.getAmountTotal()).thenReturn(5000L);

        PaymentTransaction pendingTransaction = new PaymentTransaction();
        pendingTransaction.setStatus(TransactionStatus.PENDING);
        
        when(paymentTransactionRepository.findByStripeCheckoutSessionId("cs_test_123")).thenReturn(Optional.of(pendingTransaction));
        
        paymentService.handleAsyncPaymentFailed(session);

        assertThat(pendingTransaction.getStatus()).isEqualTo(TransactionStatus.FAILED);
        verify(paymentTransactionRepository).save(pendingTransaction);
        verify(paymentStatusDispatcher).dispatchPaymentStatus(any(PaymentStatusUpdateV1.class));
        verify(userBalanceRepository, never()).save(any()); // Balance should not be touched
    }
    
    @Test
    void givenTransactionExists_whenHandleChargeDisputeCreated_thenDeductsBalanceAndUpdatesStatus() {
        String userId = "user-123";
        String paymentIntentId = "pi_123";
        long disputedAmount = 2000L;
        
        Charge charge = mock(Charge.class);
        when(charge.getPaymentIntent()).thenReturn(paymentIntentId);
        when(charge.getAmount()).thenReturn(disputedAmount);

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setUserId(userId);
        transaction.setStatus(TransactionStatus.COMPLETED);

        UserBalance userBalance = new UserBalance(userId);
        userBalance.setBalanceInCents(5000L);

        when(paymentTransactionRepository.findByPaymentIntentId(paymentIntentId)).thenReturn(Optional.of(transaction));
        when(userBalanceRepository.findByUserId(userId)).thenReturn(Optional.of(userBalance));

        paymentService.handleChargeDisputeCreated(charge);

        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.DISPUTED);
        assertThat(userBalance.getBalanceInCents()).isEqualTo(3000L);
        verify(paymentTransactionRepository).save(transaction);
        verify(userBalanceRepository).save(userBalance);
    }
}
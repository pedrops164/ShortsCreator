package com.payment_service.messaging;

import com.shortscreator.shared.dto.PaymentStatusUpdateV1;

public interface PaymentStatusDispatcher {
    
    /**
     * Sends a payment status update to the configured message broker.
     * This allows the orchestrator to be decoupled from the specific messaging technology (e.g., RabbitMQ, SQS).
     * 
     * @param paymentStatusUpdate The PaymentStatusUpdateV1 data transfer object containing user ID and payment status.
     */
    public void dispatchPaymentStatus(PaymentStatusUpdateV1 paymentStatusUpdate);
}

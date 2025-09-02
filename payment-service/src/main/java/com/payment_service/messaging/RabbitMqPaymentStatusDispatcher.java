package com.payment_service.messaging;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.payment_service.config.AppProperties;
import com.shortscreator.shared.dto.PaymentStatusUpdateV1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
// @Profile("dev") // Only activates this bean when the 'dev' profile is active
public class RabbitMqPaymentStatusDispatcher implements PaymentStatusDispatcher {

    private final RabbitTemplate rabbitTemplate;
    private final AppProperties appProperties;

    @Override
    public void dispatchPaymentStatus(PaymentStatusUpdateV1 paymentStatusUpdate) {
        // Implement RabbitMQ message sending logic here
        String exchangeName = appProperties.getRabbitmq().getPaymentExchange();
        String paymentStatusRoutingKey = appProperties.getRabbitmq().getRoutingKeys().getPaymentStatus();
        
        log.info("Dispatching payment status update for user {}: {}", paymentStatusUpdate.userId(), paymentStatusUpdate);
        rabbitTemplate.convertAndSend(exchangeName, paymentStatusRoutingKey, paymentStatusUpdate);
    }
}

package com.payment_service.event;

import com.payment_service.service.BalanceService;
import com.shortscreator.shared.dto.NewUserEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final BalanceService balanceService;

    // Assumes there is a queue named "q.user.new" bound to the exchange
    @RabbitListener(queues = "#{appProperties.rabbitmq.queues.newUser}")
    public void handleNewUserEvent(NewUserEvent event) {
        log.info("Received NewUserEvent for userId: {}", event.userId());
        try {
            balanceService.createNewUserBalanceWithWelcomeCredit(event.userId());
        } catch (Exception e) {
            log.error("Failed to process NewUserEvent for userId: {}", event.userId(), e);
            // In production, we might want to re-queue the message or send it to a dead-letter queue.
        }
    }
}
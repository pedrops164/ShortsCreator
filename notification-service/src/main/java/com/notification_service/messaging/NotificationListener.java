package com.notification_service.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import com.shortscreator.shared.dto.VideoStatusUpdateV1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.notification_service.controller.NotificationService;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = "#{appProperties.rabbitmq.queues.notifications}")
    public void handleVideoStatusUpdate(VideoStatusUpdateV1 update) {
        // When a message is received, call the NotificationService to push it to the frontend.
        log.info("Received video status update for user {}: {}", update.userId(), update);
        notificationService.sendNotificationToUser(update.userId(), update);
    }
}
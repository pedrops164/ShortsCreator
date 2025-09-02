package com.content_generation_service.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.content_generation_service.config.AppProperties;
import com.content_generation_service.generation.service.visual.ProgressListener;
import com.shortscreator.shared.dto.VideoStatusUpdateV1;
import com.shortscreator.shared.enums.ContentStatus; // Import ContentStatus

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitMqVideoStatusUpdateDispatcher implements VideoStatusUpdateDispatcher {

    private final RabbitTemplate rabbitTemplate;
    private final AppProperties appProperties;

    private void sendStatusUpdate(VideoStatusUpdateV1 statusUpdate) {
        String exchangeName = appProperties.getRabbitmq().getExchange();
        String statusUpdateRoutingKey = appProperties.getRabbitmq().getRoutingKeys().getContentStatus();
        log.debug("Dispatching VideoStatusUpdate to RabbitMQ exchange '{}' with routing key '{}'. ContentId: {}, Status: {}, Progress: {}", 
                 exchangeName, statusUpdateRoutingKey, statusUpdate.contentId(), statusUpdate.status(), statusUpdate.progressPercentage());
        try {
            rabbitTemplate.convertAndSend(exchangeName, statusUpdateRoutingKey, statusUpdate);
            log.debug("Successfully dispatched status update for contentId: {}", statusUpdate.contentId());
        } catch (Exception e) {
            log.error("Failed to dispatch video status update for contentId: {}", statusUpdate.contentId(), e);
            // Consider if you want to re-throw or handle more gracefully, depends on your retry strategy.
            // For a critical notification, re-throwing might be appropriate if there's an outer retry mechanism.
            throw new RuntimeException("Could not send status update message to RabbitMQ", e);
        }
    }

    @Override
    public ProgressListener forContent(String userId, String contentId) {
        // Return an anonymous implementation that "captures" userId and contentId
        return new ProgressListener() {
            @Override
            public void onProgress(double percentage) {
                VideoStatusUpdateV1 statusUpdate = new VideoStatusUpdateV1(
                    userId,
                    contentId,
                    ContentStatus.PROCESSING,
                    percentage
                );
                sendStatusUpdate(statusUpdate);
            }

            @Override
            public void onComplete() {
                VideoStatusUpdateV1 statusUpdate = new VideoStatusUpdateV1(
                    userId,
                    contentId,
                    ContentStatus.COMPLETED,
                    100.0
                );
                sendStatusUpdate(statusUpdate);
            }

            @Override
            public void onError() {
                VideoStatusUpdateV1 statusUpdate = new VideoStatusUpdateV1(
                    userId,
                    contentId,
                    ContentStatus.FAILED,
                    null
                );
                sendStatusUpdate(statusUpdate);
            }
        };
    }
}
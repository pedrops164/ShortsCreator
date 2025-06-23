package com.content_storage_service.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.content_storage_service.config.AppProperties;
import com.content_storage_service.service.ContentService;
import com.shortscreator.shared.dto.StatusUpdateV1;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatusUpdateListener {

    private final ContentService contentService;
    private final AppProperties appProperties; // Custom properties class

    //@RabbitListener(queues = "${app.rabbitmq.queues.status-updates}")
    @RabbitListener(queues = "#{appProperties.rabbitmq.queues.statusUpdates}")
    public void handleStatusUpdate(StatusUpdateV1 update) {
        log.info("Received status update for contentId {}: Status {}", update.getContentId(), update.getStatus());
        contentService.processStatusUpdate(update).subscribe(
            updatedContent -> log.info("Successfully updated content {} to status {}", updatedContent.getId(), updatedContent.getStatus()),
            error -> log.error("Failed to process status update for contentId {}: {}", update.getContentId(), error.getMessage())
        );
    }
}
package com.content_storage_service.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.content_storage_service.service.ContentService;
import com.shortscreator.shared.dto.GenerationResultV1;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationResultListener {

    private final ContentService contentService;

    @RabbitListener(queues = "#{appProperties.rabbitmq.queues.generationResults}")
    public void handleStatusUpdate(GenerationResultV1 generationResult) {
        log.info("Received status update for contentId {}: Status {}", generationResult.getContentId(), generationResult.getStatus());
        contentService.processGenerationResult(generationResult).subscribe(
            updatedContent -> log.info("Successfully updated content {} to status {}", updatedContent.getId(), updatedContent.getStatus()),
            error -> log.error("Failed to process status update for contentId {}: {}", generationResult.getContentId(), error.getMessage())
        );
    }
}
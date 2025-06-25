package com.content_generation_service.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

import com.shortscreator.shared.dto.GenerationRequestV1;
import com.shortscreator.shared.dto.OutputAssetsV1;
import com.shortscreator.shared.dto.StatusUpdateV1;
import com.shortscreator.shared.enums.ContentStatus;
import com.shortscreator.shared.validation.TemplateValidator;
import com.content_generation_service.config.AppProperties; // Custom properties class

@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationRequestListener {

    private final RabbitTemplate rabbitTemplate;
    private final AppProperties appProperties; // Custom properties class
    private final TemplateValidator templateValidator; // Inject validator bean

    // Note: You must configure a MessageConverter bean that uses Jackson for this to work with JsonNode out-of-the-box.
    // Spring Boot's auto-configuration for AMQP usually does this if Jackson is on the classpath.
    @RabbitListener(queues = "#{appProperties.rabbitmq.queues.generationRequests}")
    public void handleGenerationRequest(GenerationRequestV1 request) {
        log.info("Received generation request for contentId: {}", request.getContentId());

        String exchangeName = appProperties.getRabbitmq().getExchange();
        String statusUpdateRoutingKey = appProperties.getRabbitmq().getRoutingKeys().getStatusUpdate();

        // FAKE GENERATION PROCESS
        try {
            // Re-validate the parameters here against the given template.
            templateValidator.validate(request.getTemplateId(), request.getTemplateParams(), true);
            // Simulate I/O bound work (e.g., calling external APIs, file processing)
            Thread.sleep(Duration.ofSeconds(1).toMillis());

            // Simulate success
            boolean isSuccess = true;

            if (isSuccess) {
                log.info("Successfully generated content for {}", request.getContentId());
                // Create dummy output assets
                String videoUrl = "https://fake-storage.com/videos/" + request.getContentId() + ".mp4";
                OutputAssetsV1 outputAssets = new OutputAssetsV1(videoUrl, 58);
                StatusUpdateV1 update = new StatusUpdateV1(request.getContentId(), ContentStatus.COMPLETED, outputAssets, null);
                String routingKey = statusUpdateRoutingKey + ".completed"; // "update.status.completed"
                rabbitTemplate.convertAndSend(exchangeName, routingKey, update);
                log.info("Sent COMPLETED status update for {}", request.getContentId());
            } else {
                throw new RuntimeException("A simulated random error occurred during generation.");
            }

        } catch (Exception e) {
            log.error("Failed to generate content for {}: {}", request.getContentId(), e.getMessage());
            StatusUpdateV1 update = new StatusUpdateV1(request.getContentId(), ContentStatus.FAILED, null, "Generation failed: " + e.getMessage());
            String routingKey = statusUpdateRoutingKey + ".failed"; // "update.status.failed"
            rabbitTemplate.convertAndSend(exchangeName, routingKey, update);
            log.info("Sent FAILED status update for {}", request.getContentId());
        }
    }
}
package com.content_generation_service.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.shortscreator.shared.dto.GenerationRequestV1;
import com.shortscreator.shared.dto.OutputAssetsV1;
import com.shortscreator.shared.dto.StatusUpdateV1;
import com.shortscreator.shared.enums.ContentStatus;
import com.shortscreator.shared.validation.TemplateValidator;
import com.content_generation_service.config.AppProperties; // Custom properties class
import com.content_generation_service.generation.orchestrator.RedditStoryOrchestrator;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationRequestListener {

    private final RabbitTemplate rabbitTemplate;
    private final AppProperties appProperties; // Custom properties class
    private final TemplateValidator templateValidator; // Inject validator bean
    private final RedditStoryOrchestrator redditStoryOrchestrator; // Inject orchestrator bean

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

            // Dispatch to the correct orchestrator based on templateId
            if (RedditStoryOrchestrator.REDDIT_STORY_TEMPLATE_ID.equals(request.getTemplateId())) {
                
                // 2. The orchestrator does all the heavy lifting
                OutputAssetsV1 assets = redditStoryOrchestrator.generate(request.getTemplateParams());

                // 3. If it succeeds, send the COMPLETED message
                StatusUpdateV1 update = new StatusUpdateV1(request.getContentId(), ContentStatus.COMPLETED, assets, null);
                rabbitTemplate.convertAndSend(exchangeName, statusUpdateRoutingKey + ".completed", update);
                log.info("Successfully processed and sent COMPLETED update for {}", request.getContentId());
            } else {
                throw new UnsupportedOperationException("Template ID not supported: " + request.getTemplateId());
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
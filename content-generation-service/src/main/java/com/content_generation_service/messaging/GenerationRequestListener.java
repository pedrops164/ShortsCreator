package com.content_generation_service.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.shortscreator.shared.dto.GeneratedVideoDetailsV1;
import com.shortscreator.shared.dto.GenerationRequestV1;
import com.shortscreator.shared.dto.GenerationResultV1;
import com.shortscreator.shared.enums.ContentStatus;
import com.shortscreator.shared.validation.TemplateValidator;
import com.content_generation_service.generation.orchestrator.CharacterExplainsOrchestrator;
import com.content_generation_service.generation.orchestrator.RedditStoryOrchestrator;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationRequestListener {

    // Inject interface that dispatches video upload jobs to CSS
    private final GenerationResultDispatcher generationResultDispatcher;

    private final TemplateValidator templateValidator; // Inject validator bean
    private final RedditStoryOrchestrator redditStoryOrchestrator; // Inject orchestrator bean
    private final CharacterExplainsOrchestrator characterExplainsOrchestrator;

    // Must configure a MessageConverter bean that uses Jackson for this to work with JsonNode out-of-the-box.
    // Spring Boot's auto-configuration for AMQP usually does this if Jackson is on the classpath.
    @RabbitListener(queues = "#{appProperties.rabbitmq.queues.generationRequests}")
    public void handleGenerationRequest(GenerationRequestV1 request) {
        log.info("Received generation request for contentId: {}", request.getContentId());

        try {
            // Re-validate the parameters here against the given template.
            templateValidator.validate(request.getTemplateId(), request.getTemplateParams(), true);

            // Dispatch to the correct orchestrator based on templateId
            if (RedditStoryOrchestrator.REDDIT_STORY_TEMPLATE_ID.equals(request.getTemplateId())) {
                
                // The orchestrator does all the heavy lifting
                GeneratedVideoDetailsV1 videoDetails = redditStoryOrchestrator.generate(request.getTemplateParams(), request.getContentId(), request.getUserId());
                GenerationResultV1 generationResult = new GenerationResultV1(
                    request.getContentId(),
                    ContentStatus.COMPLETED,
                    videoDetails,
                    null // No error message since this is a successful job
                );
                // send job to CSS
                generationResultDispatcher.dispatch(generationResult);
                log.info("Successfully dispatched video upload job for contentId: {}", request.getContentId());
            } else if (CharacterExplainsOrchestrator.CHARACTER_EXPLAINS_TEMPLATE_ID.equals(request.getTemplateId())) {
                // Handle the "Character Explains" template
                GeneratedVideoDetailsV1 videoDetails = characterExplainsOrchestrator.generate(request.getTemplateParams(), request.getContentId(), request.getUserId());
                GenerationResultV1 generationResult = new GenerationResultV1(
                    request.getContentId(),
                    ContentStatus.COMPLETED,
                    videoDetails,
                    null // No error message since this is a successful job
                );
                // send job to CSS
                generationResultDispatcher.dispatch(generationResult);
                log.info("Successfully dispatched video upload job for contentId: {}", request.getContentId());
            } else {
                throw new UnsupportedOperationException("Template ID not supported: " + request.getTemplateId());
            }
        } catch (Exception e) {
            log.error("Failed to generate content for {}: {}", request.getContentId(), e.getMessage());

            GenerationResultV1 generationResult = new GenerationResultV1(
                request.getContentId(),
                ContentStatus.FAILED,
                null, // No job since this is a failure
                "Video composition failed: " + e.getMessage()
            );
            // send failure result to CSS
            generationResultDispatcher.dispatch(generationResult);
            log.info("Dispatched failure result for contentId: {}", request.getContentId());
        }
    }
}
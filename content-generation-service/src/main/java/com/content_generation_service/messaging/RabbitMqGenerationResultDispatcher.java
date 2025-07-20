package com.content_generation_service.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.content_generation_service.config.AppProperties;
import com.shortscreator.shared.dto.GenerationResultV1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
@RequiredArgsConstructor
@Profile("dev") // Only activates this bean when the 'local' profile is active
public class RabbitMqGenerationResultDispatcher implements GenerationResultDispatcher {

    private final RabbitTemplate rabbitTemplate;
    private final AppProperties appProperties;

    @Override
    public void dispatch(GenerationResultV1 generationResult) {
        String exchangeName = appProperties.getRabbitmq().getExchange();
        String generationResultRoutingKey = appProperties.getRabbitmq().getRoutingKeys().getGenerationResult();
        log.info("Dispatching VideoUploadJob to RabbitMQ exchange '{}' with routing key '{}'. ContentId: {}", 
                 exchangeName, generationResultRoutingKey, generationResult.getContentId());
        try {
            rabbitTemplate.convertAndSend(exchangeName, generationResultRoutingKey, generationResult);
            log.info("Successfully dispatched job for contentId: {}", generationResult.getContentId());
        } catch (Exception e) {
            log.error("Failed to dispatch video upload job for contentId: {}", generationResult.getContentId(), e);
            throw new RuntimeException("Could not send message to RabbitMQ", e);
        }
    }
}

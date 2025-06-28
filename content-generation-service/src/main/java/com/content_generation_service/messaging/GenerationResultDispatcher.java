package com.content_generation_service.messaging;

import com.shortscreator.shared.dto.GenerationResultV1;

/**
 * An abstraction for dispatching video upload jobs to a message queue.
 * This allows the orchestrator to be decoupled from the specific messaging technology (e.g., RabbitMQ, SQS).
 */
public interface GenerationResultDispatcher {
    
    /**
     * Sends a generation result dto to the configured message broker.
     * @param generationResult The GenerationResultV1 data transfer object containing job details.
     */
    public void dispatch(GenerationResultV1 generationResult);
}

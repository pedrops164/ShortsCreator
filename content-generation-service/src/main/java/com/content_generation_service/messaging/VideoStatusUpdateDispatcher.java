package com.content_generation_service.messaging;

import com.content_generation_service.generation.service.visual.ProgressListener;

/**
 * An abstraction for dispatching video upload jobs to a message queue.
 * This allows the orchestrator to be decoupled from the specific messaging technology (e.g., RabbitMQ, SQS).
 */
public interface VideoStatusUpdateDispatcher {

    /**
     * Returns a specialized dispatcher instance for a specific content generation process,
     * pre-configured with userId and contentId.
     * This avoids passing userId and contentId repeatedly to progress updates.
     *
     * @param userId The ID of the user.
     * @param contentId The ID of the content being processed.
     * @return A scoped FfmpegProgressListener that uses the internal dispatcher.
     */
    ProgressListener forContent(String userId, String contentId);
}

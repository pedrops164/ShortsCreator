package com.notification_service.controller;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.shortscreator.shared.dto.PaymentStatusUpdateV1;
import com.shortscreator.shared.dto.VideoStatusUpdateV1;

@Service
@RestController
@RequestMapping("/api/v1/notifications")
@Slf4j
public class NotificationService {
    
    // A thread-safe map to store emitters for each user. This is crucial for production.
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * Handles SSE connection requests from clients.
     */
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter handleSseConnection(@RequestHeader("X-User-ID") String userId) {
        // Set a long timeout, as SSE connections are long-lived.
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        this.emitters.put(userId, emitter);
        log.info("SSE connection established for user: {}", userId);

        // Remove the emitter from the map on completion or timeout to prevent memory leaks.
        emitter.onCompletion(() -> {
            this.emitters.remove(userId);
            log.info("SSE connection closed for user: {}", userId);
        });
        emitter.onTimeout(() -> {
            this.emitters.remove(userId);
            log.info("SSE connection timed out for user: {}", userId);
        });

        // Send a confirmation event to the client upon connection.
        try {
            emitter.send(SseEmitter.event().name("connected").data("Connection established"));
        } catch (IOException e) {
            log.error("Error sending connection confirmation to user {}: {}", userId, e.getMessage());
            emitter.complete();
        }

        return emitter;
    }

    /**
     * Sends a notification to a specific user. This method is called by the RabbitMQ listener.
     */
    public void sendNotificationToUser(String userId, VideoStatusUpdateV1 data) {
        SseEmitter emitter = emitters.get(userId);
        
        if (emitter != null) {
            try {
                // The 'name' helps the frontend distinguish between different event types.
                emitter.send(SseEmitter.event().name("video_status_update").data(data));
                log.info("Sent notification to user {}: {}", userId, data);
            } catch (IOException e) {
                log.error("Failed to send notification to user {}. Removing emitter. Error: {}", userId, e.getMessage());
                // If sending fails, the client has likely disconnected. Remove the stale emitter.
                emitter.complete();
                emitters.remove(userId);
            }
        } else {
            log.warn("No active SSE connection found for user: {}", userId);
        }
    }

    public void sendPaymentStatusUpdate(String userId, PaymentStatusUpdateV1 data) {
        SseEmitter emitter = emitters.get(userId);
        
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("payment_status_update").data(data));
                log.info("Sent payment status update to user {}: {}", userId, data);
            } catch (IOException e) {
                log.error("Failed to send payment status update to user {}. Removing emitter. Error: {}", userId, e.getMessage());
                emitter.complete();
                emitters.remove(userId);
            }
        } else {
            log.warn("No active SSE connection found for user: {}", userId);
        }
    }
}
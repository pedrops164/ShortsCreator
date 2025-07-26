package com.content_generation_service.config;

import lombok.Data;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Component
@ConfigurationProperties(prefix = "app")
@Data // Lombok for getters/setters
@Validated // Enables validation on the properties
public class AppProperties {

    private RabbitMQ rabbitmq = new RabbitMQ();

    @Valid
    @NotNull
    private Tts tts = new Tts();

    @Valid
    @NotNull
    private Transcription transcription = new Transcription();
    
    @Data
    public static class Transcription {
        @NotEmpty
        private String provider; // e.g., "openai"
    }
    
    @Data
    public static class Tts {
        private Openai openai = new Openai();
        private Elevenlabs elevenlabs = new Elevenlabs();
        private Speechify speechify = new Speechify();
    }

    @Data
    public static class SpeechifyVoiceMapping {
        private String rick;
        private String morty;
        private String peter;
        private String stewie; // Optional, can be null if not set
    }
    
    @Data
    public static class Openai {
        private String apiKey;
    }
    @Data
    public static class Elevenlabs {
        private String apiKey;
    }

    @Data
    public static class Speechify {
        private String apiKey;
        // Spring Boot automatically populates this map from application.yml
        private Map<String, String> voiceMapping;
    }

    @Data
    public static class RabbitMQ {
        @NotEmpty
        private String exchange;
        private Queues queues = new Queues();
        private RoutingKeys routingKeys = new RoutingKeys();
    }

    @Data
    public static class Queues {
        @NotEmpty
        private String generationRequests;
    }

    @Data
    public static class RoutingKeys {
        @NotEmpty
        private String generationResult;
        @NotEmpty
        private String generationRequestPrefix; // e.g., "request.generate."
        @NotEmpty
        private String contentStatus; // e.g., "content.status"
    }
}
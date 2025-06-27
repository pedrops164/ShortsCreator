package com.content_generation_service.config;

import lombok.Data;
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
        @NotEmpty
        private String provider; // e.g., "openai"
        private Openai openai = new Openai();
    }
    
    @Data
    public static class Openai {
        private String apiKey;
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
        @NotEmpty
        private String statusUpdates;
    }

    @Data
    public static class RoutingKeys {
        @NotEmpty
        private String generationRequestPrefix;
        @NotEmpty
        private String statusUpdate;
    }
}
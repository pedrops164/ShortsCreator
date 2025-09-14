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
    private Openai openai = new Openai();
    
    @Data
    public static class Openai {
        @NotEmpty
        private String apiKey;
        @Valid
        @NotNull
        private LLM llm;
    }

    @Data
    public static class LLM {
        @NotEmpty
        private String url; // Base URL for the OpenAI API
        @NotEmpty
        private String model; // e.g., "gpt-3.5-turbo"
        private Double temperature;
        private Integer maxTokens;
    }

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
        private Elevenlabs elevenlabs = new Elevenlabs();
        private Speechify speechify = new Speechify();
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

    @Valid
    @NotNull
    private Video video = new Video();

    @Data
    public static class Video {
        private int width;
        private int height;
    }

    private Assets assets = new Assets();

    @Data
    public static class Assets {
        @NotEmpty
        private String videos; // e.g., "assets/videos/"
        @NotEmpty
        private String characters; // e.g., "assets/images/characters/"
        @NotEmpty
        private String audio; // e.g., "assets/audio/"
        @NotEmpty
        private String redditImages; // e.g., "assets/images/reddit/"
        @NotEmpty
        private String images; // e.g., "assets/images/"
        @NotEmpty
        private String fonts; // e.g., "assets/fonts/"
    }

    private Google google = new Google();

    @Data
    public static class Google {
        @NotEmpty
        private String apiKey;
        @NotEmpty
        private String cseId;
    }

}
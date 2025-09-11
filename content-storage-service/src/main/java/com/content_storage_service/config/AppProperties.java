package com.content_storage_service.config;

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
        private String generationResults;
    }

    @Data
    public static class RoutingKeys {
        @NotEmpty
        private String generationRequestPrefix;
        @NotEmpty
        private String generationResult;
    }

    // Properties for Services configuration
    private Services services = new Services();

    @Data
    public static class Services {
        private PaymentService paymentService = new PaymentService();
    }

    @Data
    public static class PaymentService {
        @NotEmpty
        private String url; // URL for the payment service, used by the CSS to manage user balances.
    }

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
}
package com.content_generation_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;

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
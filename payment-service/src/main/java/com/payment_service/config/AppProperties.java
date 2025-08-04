package com.payment_service.config;

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
        private String paymentExchange;
        @NotEmpty
        private RoutingKeys routingKeys = new RoutingKeys();
    }

    @Data
    public static class RoutingKeys {
        @NotEmpty
        private String paymentStatus; // e.g., "payment.status"
    }
}
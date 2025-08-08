package com.keycloak_event_listener.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppProperties {
    private RabbitMQ rabbitmq = new RabbitMQ();

    @Getter
    @Setter
    public static class RabbitMQ {
        private String host;
        private int port;
        private String username;
        private String password;
        private String virtualHost;
        private String exchangeName;
        private String newUserRoutingKey;
    }
}
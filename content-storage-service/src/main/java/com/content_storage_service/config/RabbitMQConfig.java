package com.content_storage_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.Queue;
//import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class RabbitMQConfig {

    private final AppProperties appProperties; // 1. Inject the properties bean

    @Bean
    public TopicExchange contentExchange() {
        // 2. Use the getter from the properties class
        return new TopicExchange(appProperties.getRabbitmq().getExchange());
    }

    @Bean
    public Queue statusUpdateQueue() {
        return new Queue(appProperties.getRabbitmq().getQueues().getStatusUpdates(), true);
    }

    @Bean
    public Binding statusUpdateBinding(Queue statusUpdateQueue, TopicExchange contentExchange) {
        // Bind to any message starting with "update.status."
        return BindingBuilder.bind(statusUpdateQueue)
                            .to(contentExchange)
                            .with(appProperties.getRabbitmq().getRoutingKeys().getStatusUpdate() + ".#"); 
    }
}
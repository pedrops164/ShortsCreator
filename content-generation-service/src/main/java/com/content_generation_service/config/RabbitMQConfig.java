package com.content_generation_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class RabbitMQConfig {

    private final AppProperties appProperties; // 1. Inject the properties bean

    // We will listen for reddit_story_v1 requests specifically
    private static final String REDDIT_STORY_ROUTING_KEY = "request.generate.reddit_story_v1";

    @Bean
    public TopicExchange contentExchange() {
        // 2. Use the getter from the properties class
        return new TopicExchange(appProperties.getRabbitmq().getExchange());
    }

    @Bean
    public Queue generationRequestQueue() {
        return new Queue(appProperties.getRabbitmq().getQueues().getGenerationRequests(), true);
    }

    @Bean
    public Binding generationBinding(Queue generationRequestQueue, TopicExchange contentExchange) {
        // Bind the queue to the exchange for the specific templateId we support
        return BindingBuilder.bind(generationRequestQueue)
                             .to(contentExchange)
                             .with(REDDIT_STORY_ROUTING_KEY);
    }
}
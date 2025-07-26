package com.content_generation_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class RabbitMQConfig {

    private final AppProperties appProperties; // 1. Inject the properties bean

    /**
     * Creates a bean for the Jackson2JsonMessageConverter.
     * This converter will serialize objects to JSON format.
     * @return A MessageConverter bean.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Creates a RabbitTemplate bean and configures it to use the
     * Jackson2JsonMessageConverter.
     * Spring will use this template whenever we autowire RabbitTemplate.
     * @param connectionFactory The auto-configured RabbitMQ connection factory.
     * @return A configured RabbitTemplate.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

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
    public Binding redditStoryBinding(Queue generationRequestQueue, TopicExchange contentExchange) {
        final String generationRequestPrefix = appProperties.getRabbitmq().getRoutingKeys().getGenerationRequestPrefix();
        // Bind the queue to the exchange for the specific templateId we support
        // Use '#' wildcard to match any routing key that starts with the generationRequestPrefix.
        return BindingBuilder.bind(generationRequestQueue)
                             .to(contentExchange)
                             .with(generationRequestPrefix + "#");
    }
}
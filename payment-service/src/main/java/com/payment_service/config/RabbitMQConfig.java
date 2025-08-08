package com.payment_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.DirectExchange;
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

    private final AppProperties appProperties; // Inject the properties bean

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
    public DirectExchange keycloakExchange() {
        return new DirectExchange(appProperties.getRabbitmq().getKeycloakExchange());
    }

    @Bean
    public Queue newUsersQueue() {
        return new Queue(appProperties.getRabbitmq().getQueues().getNewUser(), true);
    }

    @Bean
    public Binding newUsersBinding(Queue newUsersQueue, DirectExchange keycloakExchange) {
        // Bind to any message with the given routing key
        return BindingBuilder.bind(newUsersQueue)
                            .to(keycloakExchange)
                            .with(appProperties.getRabbitmq().getRoutingKeys().getNewUser());
    }
}
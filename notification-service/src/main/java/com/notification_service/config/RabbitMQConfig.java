package com.notification_service.config;

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

    // ---- Configurations for content notifications ----

    /**
     * Creates a TopicExchange bean for notifications.
     * @return A TopicExchange bean for notifications.
     */
    @Bean
    TopicExchange exchange() {
        return new TopicExchange(appProperties.getRabbitmq().getExchange());
    }

    /**
     * Creates a Queue bean for notifications.
     * The queue is durable, meaning it will survive broker restarts.
     * @return A Queue bean for notifications.
     */
    @Bean
    Queue notificationsQueue() {
        // durable: true, exclusive: false, autoDelete: false
        return new Queue(appProperties.getRabbitmq().getQueues().getNotifications(), true);
    }

    /**
     * Creates a Binding bean for notifications.
     * @param notificationsQueue The notifications queue.
     * @param exchange The topic exchange.
     * @return A Binding bean for notifications.
     */
    @Bean
    Binding binding(Queue notificationsQueue, TopicExchange exchange) {
        return BindingBuilder.bind(notificationsQueue).to(exchange).with(appProperties.getRabbitmq().getRoutingKeys().getContentStatus());
    }

    // ---- Configurations for payments ----

    /**
     * Creates a TopicExchange bean for payment notifications.
     * @return A TopicExchange bean for payment notifications.
     */
    @Bean
    TopicExchange paymentExchange() {
        return new TopicExchange(appProperties.getRabbitmq().getPaymentExchange());
    }

    /**
     * Creates a Queue bean for payments.
     * The queue is durable, meaning it will survive broker restarts.
     * @return A Queue bean for payments.
     */
    @Bean
    Queue paymentsQueue() {
        return new Queue(appProperties.getRabbitmq().getQueues().getPayments(), true);
    }

    /**
     * Creates a Binding bean for payments.
     * @param paymentsQueue The payments queue.
     * @param exchange The topic exchange.
     * @return A Binding bean for payments.
     */
    @Bean
    Binding paymentBinding(Queue paymentsQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(paymentsQueue).to(paymentExchange).with(appProperties.getRabbitmq().getRoutingKeys().getPaymentStatus());
    }
}
package com.content_generation_service.messaging;

import com.content_generation_service.config.AppProperties;
import com.shortscreator.shared.dto.GenerationRequestV1;
import com.shortscreator.shared.dto.StatusUpdateV1;
import com.shortscreator.shared.enums.ContentStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.shortscreator.shared.validation.TemplateValidator;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@SpringBootTest
@Testcontainers // Activates Testcontainers extension for JUnit 5
class GenerationRequestListenerIntegrationTest {

    @Container // Manages the lifecycle of the container
    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:4.1.1-management");

    // This method dynamically sets the properties for Spring to connect to the test container
    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQContainer::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMQContainer::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMQContainer::getAdminPassword);
    }
    
    // Since the validator is now part of the app context, we need to provide a mock for it
    // during testing, otherwise the application context will fail to start.
    @TestConfiguration
    static class TestConfig {
        @Bean
        public TemplateValidator templateValidator() {
            return mock(TemplateValidator.class);
        }

        // This tells the test's ApplicationContext to use a JSON message converter,
        // overriding the default SimpleMessageConverter.
        @Bean
        public MessageConverter jackson2MessageConverter() {
            return new Jackson2JsonMessageConverter();
        }
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    // Injecting the AmqpAdmin bean allows us to manage the broker
    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private AppProperties appProperties;

    final String statusQueueName = "q.status.updates";
    final String generationRequestPrefix = "request.generate."; // This is the prefix for generation requests

    // Use @BeforeEach to ensure the queue exists before each test runs
    @BeforeEach
    void setUp() {
        // We define the infrastructure our listener will publish to.
        final String exchangeName = appProperties.getRabbitmq().getExchange();
        final String statusRoutingKey = appProperties.getRabbitmq().getRoutingKeys().getStatusUpdate();

        TopicExchange exchange = new TopicExchange(exchangeName);
        Queue queue = new Queue(statusQueueName, true);
        Binding binding = BindingBuilder.bind(queue)
                                      .to(exchange)
                                      .with(statusRoutingKey + ".#"); // e.g. update.status.#

        // Use AmqpAdmin to declare them on the broker.
        // This is idempotent; it won't fail if they already exist.
        amqpAdmin.declareExchange(exchange);
        amqpAdmin.declareQueue(queue);
        amqpAdmin.declareBinding(binding);
    }


    @Test
    void shouldProcessMessageAndSendStatusUpdate() throws InterruptedException, com.fasterxml.jackson.core.JsonProcessingException {
        // --- ARRANGE ---
        // The listener in the app will send a status update. We need to listen for it.
        // A simple way is to use the RabbitTemplate itself to receive from the status queue.
        final String exchangeName = appProperties.getRabbitmq().getExchange();
        final String generationRoutingKey = generationRequestPrefix + "reddit_story_v1";

        GenerationRequestV1 request = new GenerationRequestV1("content-id-999", "user-id-777", "reddit_story_v1", null);

        // --- ACT ---
        // Send a message to the input queue that our main listener is watching
        rabbitTemplate.convertAndSend(exchangeName, generationRoutingKey, request);

        // --- ASSERT ---
        // Wait for a response on the status update queue, waiting for a specified timeout
        StatusUpdateV1 statusUpdate = (StatusUpdateV1) rabbitTemplate.receiveAndConvert(statusQueueName, 20000);

        assertThat(statusUpdate).isNotNull();
        assertThat(statusUpdate.getContentId()).isEqualTo("content-id-999");
        // Might be one of the following outcomes
        assertThat(statusUpdate.getStatus()).isIn(ContentStatus.COMPLETED, ContentStatus.FAILED);

        if (ContentStatus.COMPLETED.equals(statusUpdate.getStatus())) {
            assertThat(statusUpdate.getOutputAssets()).isNotNull();
            assertThat(statusUpdate.getErrorMessage()).isNull();
        } else {
            assertThat(statusUpdate.getOutputAssets()).isNull();
            assertThat(statusUpdate.getErrorMessage()).isNotNull();
        }
    }
}
package com.content_generation_service.messaging;

import com.content_generation_service.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import com.fasterxml.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class GenerationRequestListenerValidationIntegrationTest {

    // Because of the absence of @TestConfiguration, the real ApplicationContext
    // will be used, which includes all the beans and configurations.
    @Container
    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:4.1.1-management"); // Sticking to standard versioning

    // dynamically sets the properties for Spring to connect to the test container
    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQContainer::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMQContainer::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMQContainer::getAdminPassword);
    }

    @TestConfiguration
    static class TestConfig {
        // This tells the test's ApplicationContext to use a JSON message converter,
        // overriding the default SimpleMessageConverter.
        @Bean
        public MessageConverter jackson2MessageConverter() {
            return new Jackson2JsonMessageConverter();
        }
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private AppProperties appProperties;
    @Autowired
    private AmqpAdmin amqpAdmin;
    @Autowired
    private ObjectMapper objectMapper;

    final String statusQueueName = "q.status.updates";
    final String generationRequestPrefix = "request.generate."; // This is the prefix for generation requests

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
    void whenMessageHasInvalidParams_thenSendsFailedStatusUpdate() throws Exception {
        // --- ARRANGE ---
        String exchangeName = appProperties.getRabbitmq().getExchange();
        String routingKey = generationRequestPrefix + "reddit_story_v1";

        // This payload has all the other required fields but is specifically missing "postTitle".
        String invalidParamsJson = """
        {
          "username": "test-user",
          "subreddit": "r/askreddit",
          "postDescription": "A test post description",
          "comments": [ { "author": "c-user", "text": "a comment" } ],
          "voiceSelection": "en-US-Standard-C",
          "backgroundVideoId": "minecraft_parkour_1",
          "aspectRatio": "9:16",
          "showSubtitles": true,
          "subtitlesFont": "Roboto",
          "subtitlesColor": "yellow",
          "subtitlesPosition": "bottom"
        }
        """;
        JsonNode invalidParams = objectMapper.readTree(invalidParamsJson);
        GenerationRequestV1 request = new GenerationRequestV1("content-id-invalid", "user-id-invalid", "reddit_story_v1", invalidParams);

        // --- ACT ---
        rabbitTemplate.convertAndSend(exchangeName, routingKey, request);

        // --- ASSERT ---
        StatusUpdateV1 statusUpdate = (StatusUpdateV1) rabbitTemplate.receiveAndConvert(statusQueueName, 20000);

        assertThat(statusUpdate).isNotNull();
        assertThat(statusUpdate.getStatus()).isEqualTo(ContentStatus.FAILED);
        assertThat(statusUpdate.getContentId()).isEqualTo("content-id-invalid");
        assertThat(statusUpdate.getErrorMessage())
            .contains("object has missing required properties", "postTitle");
    }

    @Test
    void whenMessageHasValidParams_thenSendsCompletedStatusUpdate() throws Exception {
        // --- ARRANGE ---
        String exchangeName = appProperties.getRabbitmq().getExchange();
        String routingKey = generationRequestPrefix + "reddit_story_v1";

        // This payload has all the other required fields but is specifically missing "postTitle".
        String invalidParamsJson = """
        {
          "postTitle": "Post title",
          "username": "test-user",
          "subreddit": "r/askreddit",
          "postDescription": "A test post description",
          "comments": [],
          "voiceSelection": "openai_echo",
          "backgroundVideoId": "minecraft_bg_1",
          "aspectRatio": "9:16",
          "showSubtitles": true,
          "subtitlesFont": "Roboto",
          "subtitlesColor": "#FFFFFF",
          "subtitlesPosition": "bottom"
        }
        """;
        JsonNode invalidParams = objectMapper.readTree(invalidParamsJson);
        GenerationRequestV1 request = new GenerationRequestV1("content-id", "user-id-valid", "reddit_story_v1", invalidParams);

        // --- ACT ---
        rabbitTemplate.convertAndSend(exchangeName, routingKey, request);

        // --- ASSERT ---
        StatusUpdateV1 statusUpdate = (StatusUpdateV1) rabbitTemplate.receiveAndConvert(statusQueueName, 2000000);

        assertThat(statusUpdate).isNotNull();
        assertThat(statusUpdate.getStatus()).isEqualTo(ContentStatus.COMPLETED);
        assertThat(statusUpdate.getContentId()).isEqualTo("content-id");
        assertThat(statusUpdate.getOutputAssets()).isNotNull();
        assertThat(statusUpdate.getErrorMessage()).isNull();
    }
}
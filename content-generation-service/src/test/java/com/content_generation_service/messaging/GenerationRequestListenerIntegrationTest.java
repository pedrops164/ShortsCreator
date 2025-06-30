package com.content_generation_service.messaging;

import com.content_generation_service.config.AppProperties;
import com.content_generation_service.generation.orchestrator.RedditStoryOrchestrator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shortscreator.shared.dto.GenerationRequestV1;
import com.shortscreator.shared.dto.GenerationResultV1;
import com.shortscreator.shared.dto.VideoUploadJobV1;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers // Activates Testcontainers extension for JUnit 5
class GenerationRequestListenerIntegrationTest {

    @Container // Manages the lifecycle of the container
    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:4.1.1-management");
    @Autowired
    private ObjectMapper objectMapper;

    // This method dynamically sets the properties for Spring to connect to the test container
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
    @MockitoBean
    private RedditStoryOrchestrator redditStoryOrchestrator;
    
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
        final String generationResultRoutingKey = appProperties.getRabbitmq().getRoutingKeys().getGenerationResult();

        TopicExchange exchange = new TopicExchange(exchangeName);
        Queue queue = new Queue(statusQueueName, true);
        Binding binding = BindingBuilder.bind(queue)
                                      .to(exchange)
                                      .with(generationResultRoutingKey);

        // Use AmqpAdmin to declare them on the broker.
        // This is idempotent; it won't fail if they already exist.
        amqpAdmin.declareExchange(exchange);
        amqpAdmin.declareQueue(queue);
        amqpAdmin.declareBinding(binding);
    }


    @Test
    void shouldProcessMessageAndSendStatusUpdate() throws InterruptedException, com.fasterxml.jackson.core.JsonProcessingException {
        // --- ARRANGE ---
        // The listener in the app will send a generation result. We need to listen for it.
        // A simple way is to use the RabbitTemplate itself to receive from the status queue.
        final String exchangeName = appProperties.getRabbitmq().getExchange();
        final String generationRoutingKey = generationRequestPrefix + "reddit_story_v1";

        // create request whose validation will fail (templateParams are null)
        GenerationRequestV1 request = new GenerationRequestV1("content-id-999", "user-id-777", "reddit_story_v1", null);

        // --- ACT ---
        // Send a message to the input queue that our main listener is watching
        rabbitTemplate.convertAndSend(exchangeName, generationRoutingKey, request);

        // --- ASSERT ---
        // Wait for a response on the generation result queue, waiting for a specified timeout
        GenerationResultV1 generationResult = (GenerationResultV1) rabbitTemplate.receiveAndConvert(statusQueueName, 20000);

        assertThat(generationResult).isNotNull();
        assertThat(generationResult.getContentId()).isEqualTo("content-id-999");
        assertThat(generationResult.getStatus()).isEqualTo(ContentStatus.FAILED);
        assertThat(generationResult.getErrorMessage()).isNotNull();
    }

    @Test
    void whenMessageHasInvalidParams_thenSendsFailedStatusUpdate() throws Exception {
        // --- ARRANGE ---
        final String exchangeName = appProperties.getRabbitmq().getExchange();
        final String generationRoutingKey = generationRequestPrefix + "reddit_story_v1";

        // This payload has all the other required fields but is specifically missing "postTitle".
        String invalidParamsJson = """
        {
          "username": "ask-dev",
          "subreddit": "r/askreddit",
          "theme": "dark",
          "avatarImageUrl": "assets/reddit/reddit_avatar_placeholder.png",
          "postDescription": "Sometimes the best wisdom is hidden in humor. What have you got?",
          "comments": [
            { "author": "user1", "text": "Always borrow money from a pessimist. They'll never expect it back." },
            { "author": "user2", "text": "Never wrestle with a pig. You both get dirty and besides, the pig likes it." }
          ],
          "voiceSelection": "alloy",
          "backgroundVideoId": "minecraft1",
          "aspectRatio": "9:16",
          "showSubtitles": true,
          "subtitlesFont": "Montserrat ExtraBold",
          "subtitlesColor": "#f6ff00",
          "subtitlesPosition": "bottom"
        }
        """;
        JsonNode invalidParams = objectMapper.readTree(invalidParamsJson);
        GenerationRequestV1 request = new GenerationRequestV1("content-id-invalid", "user-id-invalid", "reddit_story_v1", invalidParams);

        // --- ACT ---
        rabbitTemplate.convertAndSend(exchangeName, generationRoutingKey, request);

        // --- ASSERT ---
        GenerationResultV1 generationResult = (GenerationResultV1) rabbitTemplate.receiveAndConvert(statusQueueName, 10000);

        assertThat(generationResult).isNotNull();
        assertThat(generationResult.getStatus()).isEqualTo(ContentStatus.FAILED);
        assertThat(generationResult.getContentId()).isEqualTo("content-id-invalid");
        assertThat(generationResult.getErrorMessage())
            .contains("object has missing required properties", "postTitle");
    }

    @Test
    void whenMessageHasValidParams_thenSendsCompletedStatusUpdate() throws Exception {
        // --- ARRANGE ---
        final String exchangeName = appProperties.getRabbitmq().getExchange();
        final String generationRoutingKey = generationRequestPrefix + "reddit_story_v1";
        VideoUploadJobV1 mockJob = new VideoUploadJobV1();
        when(redditStoryOrchestrator.generate(any(), any(), any()))
            .thenReturn(mockJob);

        // This payload has all the required fields.
        String validParamsJson = """
        {
          "postTitle": "What's a piece of advice that sounds like a joke but is actually profound?",
          "username": "ask-dev",
          "subreddit": "r/askreddit",
          "theme": "dark",
          "avatarImageUrl": "assets/reddit/reddit_avatar_placeholder.png",
          "postDescription": "Sometimes the best wisdom is hidden in humor. What have you got?",
          "comments": [
            { "author": "user1", "text": "Always borrow money from a pessimist. They'll never expect it back." },
            { "author": "user2", "text": "Never wrestle with a pig. You both get dirty and besides, the pig likes it." }
          ],
          "voiceSelection": "alloy",
          "backgroundVideoId": "minecraft1",
          "aspectRatio": "9:16",
          "showSubtitles": true,
          "subtitlesFont": "Montserrat ExtraBold",
          "subtitlesColor": "#f6ff00",
          "subtitlesPosition": "bottom"
        }
        """;
        JsonNode validParams = objectMapper.readTree(validParamsJson);
        GenerationRequestV1 request = new GenerationRequestV1("content-id", "user-id-valid", "reddit_story_v1", validParams);

        // --- ACT ---
        rabbitTemplate.convertAndSend(exchangeName, generationRoutingKey, request);

        // --- ASSERT ---
        GenerationResultV1 generationResult = (GenerationResultV1) rabbitTemplate.receiveAndConvert(statusQueueName, 2000000);

        assertThat(generationResult).isNotNull();
        assertThat(generationResult.getStatus()).isEqualTo(ContentStatus.COMPLETED);
        assertThat(generationResult.getContentId()).isEqualTo("content-id");
        assertThat(generationResult.getErrorMessage()).isNull();
    }
}
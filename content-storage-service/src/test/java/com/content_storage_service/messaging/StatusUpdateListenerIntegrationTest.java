package com.content_storage_service.messaging;

import com.content_storage_service.config.AppProperties;
import com.content_storage_service.model.Content;
import com.content_storage_service.repository.ContentRepository;
import com.content_storage_service.service.VideoUploadProcessorService;
import com.shortscreator.shared.dto.GenerationResultV1;
import com.shortscreator.shared.dto.VideoUploadJobV1;
import com.shortscreator.shared.enums.ContentStatus;
import com.shortscreator.shared.enums.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.doNothing;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@Testcontainers
@ActiveProfiles("dev") // Usage of the 'dev' profile to get RabbitMQ/Mongo connection details
class StatusUpdateListenerIntegrationTest {

    @Container
    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:4.1.1-management");
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:8.0.10");

    @Autowired
    private AppProperties appProperties; // Inject the properties bean

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        // Override properties to point to our test containers
        registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQContainer::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMQContainer::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMQContainer::getAdminPassword);
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
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
    private ContentRepository contentRepository;
    @MockitoBean
    private VideoUploadProcessorService processorService;

    // Clean up the database after each test to ensure isolation
    @AfterEach
    void tearDown() {
        contentRepository.deleteAll().block();
    }

    @Test
    void whenCompletedStatusUpdateReceived_thenContentIsUpdatedInDb() {
        // --- ARRANGE ---
        // set processGenerationResult function to do nothing
        doNothing().when(processorService).processUploadJob(any(VideoUploadJobV1.class));
        // Create and save a document in a "processing" state to the test database.
        Content contentToUpdate = Content.builder()
                .userId("user-123")
                .status(ContentStatus.PROCESSING)
                .templateId("reddit_story_v1")
                .contentType(ContentType.REDDIT_STORY)
                .build();
        Content savedContent = contentRepository.save(contentToUpdate).block();
        assertThat(savedContent).isNotNull();
        String contentId = savedContent.getId();

        // Create the DTO for the message we're going to send.
        VideoUploadJobV1 job = new VideoUploadJobV1();
        GenerationResultV1 generationResult = new GenerationResultV1(
                contentId,
                ContentStatus.COMPLETED,
                job,
                null
        );
        final String exchangeName = appProperties.getRabbitmq().getExchange();
        final String generationResultRoutingKey = appProperties.getRabbitmq().getRoutingKeys().getGenerationResult();
        // --- ACT ---
        // Send the message to the real RabbitMQ queue that our listener is watching.
        // We send it to the exchange with the correct routing key.
        rabbitTemplate.convertAndSend(exchangeName, generationResultRoutingKey, generationResult);

        // --- ASSERT ---
        // Poll the database until our condition is met or a timeout occurs.
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Content updatedContent = contentRepository.findById(contentId).block();
            assertThat(updatedContent).isNotNull();
            assertThat(updatedContent.getStatus()).isEqualTo(ContentStatus.COMPLETED);
            assertThat(updatedContent.getErrorMessage()).isNull();
        });
    }
}

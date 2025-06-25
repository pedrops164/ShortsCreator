package com.content_storage_service.service;

import com.content_storage_service.config.AppProperties;
import com.content_storage_service.model.Content;
import com.content_storage_service.repository.ContentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shortscreator.shared.dto.GenerationRequestV1;
import com.shortscreator.shared.enums.ContentStatus;
import com.shortscreator.shared.enums.ContentType;
import com.shortscreator.shared.validation.TemplateValidator;
import com.shortscreator.shared.validation.TemplateValidator.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentServiceTest {

    @Mock
    private ContentRepository contentRepository;
    @Mock
    private TemplateValidator templateValidator;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private AppProperties appProperties; // This also needs to be mocked

    @InjectMocks // Creates an instance of ContentService and injects the mocks
    private ContentService contentService;

    private Content sampleDraft;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // Create a sample content object to be returned by the repository mock
        sampleDraft = new Content();
        sampleDraft.setId("content-id-123");
        sampleDraft.setUserId("user-id-abc");
        sampleDraft.setTemplateId("reddit_story_v1");
        sampleDraft.setStatus(ContentStatus.DRAFT);
        sampleDraft.setTemplateParams(objectMapper.createObjectNode().put("title", "A Valid Title"));
    }

    @Test
    void createDraft_whenParamsAreValid_returnsSavedContent() {
        // ARRANGE
        // Mock the validator to succeed (do nothing, as it's a void method)
        doNothing().when(templateValidator).validate(anyString(), any(), eq(false));
        // Mock the repository's save method to return the draft we created
        // This simulates the save operation returning the content with an ID
        when(contentRepository.save(any(Content.class))).thenReturn(Mono.just(sampleDraft));

        // ACT
        Mono<Content> resultMono = contentService.createDraft(
            "user-id-abc", "reddit_story_v1", ContentType.REDDIT_STORY, objectMapper.createObjectNode());

        // ASSERT
        // StepVerifier is the standard way to test reactive streams (Mono/Flux)
        StepVerifier.create(resultMono)
            .assertNext(content -> {
                // FIX 2: Make the assertion stronger and more specific.
                // Instead of just checking for not-null, check for the expected ID.
                assertThat(content).isNotNull(); // This will now pass
                assertThat(content.getId()).isEqualTo(sampleDraft.getId());
                assertThat(content.getStatus()).isEqualTo(ContentStatus.DRAFT);
            })
            .verifyComplete();
    }

    @Test
    void createDraft_whenParamsAreInvalid_returnsError() {
        // ARRANGE
        // Mock the validator to throw our custom exception
        doThrow(new ValidationException("Invalid params"))
            .when(templateValidator).validate(anyString(), any(), eq(false));

        // ACT
        Mono<Content> resultMono = contentService.createDraft(
            "user-id-abc", "reddit_story_v1", ContentType.REDDIT_STORY, null);

        // ASSERT
        StepVerifier.create(resultMono)
            .expectError(IllegalArgumentException.class) // Expect an error of this type
            .verify();
    }

    @Test
    void submitForGeneration_whenDraftIsValid_setsStatusToProcessingAndSendsMessage() {
        // ARRANGE
        when(contentRepository.findByIdAndUserId("content-id-123", "user-id-abc")).thenReturn(Mono.just(sampleDraft));
        doNothing().when(templateValidator).validate(anyString(), any(), eq(true));
        // When save is called, return the content that was passed in
        when(contentRepository.save(any(Content.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // Mock AppProperties to return dummy values for messaging
        AppProperties.RabbitMQ rabbitMQMock = mock(AppProperties.RabbitMQ.class);
        AppProperties.RoutingKeys routingKeysMock = mock(AppProperties.RoutingKeys.class);
        when(appProperties.getRabbitmq()).thenReturn(rabbitMQMock);
        when(rabbitMQMock.getExchange()).thenReturn("test-exchange");
        when(rabbitMQMock.getRoutingKeys()).thenReturn(routingKeysMock);
        when(routingKeysMock.getGenerationRequestPrefix()).thenReturn("test.prefix.");
        
        // ACT
        Mono<Content> resultMono = contentService.submitForGeneration("content-id-123", "user-id-abc");

        // ASSERT
        ArgumentCaptor<GenerationRequestV1> messageCaptor = ArgumentCaptor.forClass(GenerationRequestV1.class);

        StepVerifier.create(resultMono)
            .assertNext(content -> {
                assertThat(content.getStatus()).isEqualTo(ContentStatus.PROCESSING);
            })
            .verifyComplete();
        
        // Verify that the message was sent to RabbitMQ with the correct details
        verify(rabbitTemplate, times(1)).convertAndSend(
            eq("test-exchange"),
            eq("test.prefix.reddit_story_v1"),
            messageCaptor.capture());

        // Assert the content of the message sent
        assertThat(messageCaptor.getValue().getContentId()).isEqualTo("content-id-123");
    }
}
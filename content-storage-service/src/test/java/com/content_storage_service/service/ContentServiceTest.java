package com.content_storage_service.service;

import com.content_storage_service.config.AppProperties;
import com.content_storage_service.model.Content;
import com.content_storage_service.repository.ContentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shortscreator.shared.dto.GenerationRequestV1;
import com.shortscreator.shared.dto.GenerationResultV1;
import com.shortscreator.shared.enums.ContentStatus;
import com.shortscreator.shared.validation.TemplateValidator;
import com.shortscreator.shared.validation.TemplateValidator.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

    @Mock
    private VideoUploadProcessorService processorService; // Mock the processor service

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

    @Nested
    @DisplayName("createDraft Tests")
    class CreateDraftTests {
        @Test
        void whenParamsAreValid_returnsSavedContent() {
            // ARRANGE
            doNothing().when(templateValidator).validate(anyString(), any(), eq(false));
            when(contentRepository.save(any(Content.class))).thenReturn(Mono.just(sampleDraft));

            // ACT
            Mono<Content> resultMono = contentService.createDraft(
                "user-id-abc", "reddit_story_v1", objectMapper.createObjectNode());

            // ASSERT
            StepVerifier.create(resultMono)
                .assertNext(content -> {
                    assertThat(content).isNotNull();
                    assertThat(content.getId()).isEqualTo(sampleDraft.getId());
                    assertThat(content.getStatus()).isEqualTo(ContentStatus.DRAFT);
                })
                .verifyComplete();
        }

        @Test
        void whenParamsAreInvalid_returnsError() {
            // ARRANGE
            doThrow(new ValidationException("Invalid params")).when(templateValidator).validate(anyString(), any(), eq(false));

            // ACT
            Mono<Content> resultMono = contentService.createDraft("user-id-abc", "reddit_story_v1", null);

            // ASSERT
            StepVerifier.create(resultMono)
                .expectErrorMatches(throwable ->
                    throwable instanceof IllegalArgumentException &&
                    throwable.getMessage().contains("Initial draft parameters are invalid"))
                .verify();
        }
    }
    
    @Nested
    @DisplayName("updateDraft Tests")
    class UpdateDraftTests {
        @Test
        void whenUpdateIsValid_returnsUpdatedContent() {
            // ARRANGE
            JsonNode updatedParams = objectMapper.createObjectNode().put("title", "An Updated Title");
            doNothing().when(templateValidator).validate(anyString(), any(), eq(false));
            when(contentRepository.findByIdAndUserId(anyString(), anyString())).thenReturn(Mono.just(sampleDraft));
            when(contentRepository.save(any(Content.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
            
            // ACT
            Mono<Content> resultMono = contentService.updateDraft("content-id-123", "user-id-abc", updatedParams);

            // ASSERT
            StepVerifier.create(resultMono)
                .assertNext(content -> assertThat(content.getTemplateParams().get("title").asText()).isEqualTo("An Updated Title"))
                .verifyComplete();
        }

        @Test
        void whenUpdatingNonDraftContent_returnsError() {
            // ARRANGE
            sampleDraft.setStatus(ContentStatus.PROCESSING); // Make the content not a draft
            when(contentRepository.findByIdAndUserId(anyString(), anyString())).thenReturn(Mono.just(sampleDraft));

            // ACT
            Mono<Content> resultMono = contentService.updateDraft("content-id-123", "user-id-abc", objectMapper.createObjectNode());

            // ASSERT
            StepVerifier.create(resultMono)
                .expectError(IllegalStateException.class)
                .verify();
        }
    }

    @Nested
    @DisplayName("submitForGeneration Tests")
    class SubmitForGenerationTests {

        @Test
        void whenDraftIsValid_setsStatusToProcessingAndSendsMessage() {
            // ARRANGE
            AppProperties.RabbitMQ rabbitMQMock = mock(AppProperties.RabbitMQ.class);
            AppProperties.RoutingKeys routingKeysMock = mock(AppProperties.RoutingKeys.class);
            when(appProperties.getRabbitmq()).thenReturn(rabbitMQMock);
            when(rabbitMQMock.getExchange()).thenReturn("test-exchange");
            when(rabbitMQMock.getRoutingKeys()).thenReturn(routingKeysMock);
            when(routingKeysMock.getGenerationRequestPrefix()).thenReturn("test.prefix.");

            when(contentRepository.findByIdAndUserId("content-id-123", "user-id-abc")).thenReturn(Mono.just(sampleDraft));
            doNothing().when(templateValidator).validate(anyString(), any(), eq(true));
            when(contentRepository.save(any(Content.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
            
            // ACT
            Mono<Content> resultMono = contentService.submitForGeneration("content-id-123", "user-id-abc");

            // ASSERT
            ArgumentCaptor<GenerationRequestV1> messageCaptor = ArgumentCaptor.forClass(GenerationRequestV1.class);
            StepVerifier.create(resultMono)
                .assertNext(content -> assertThat(content.getStatus()).isEqualTo(ContentStatus.PROCESSING))
                .verifyComplete();
            verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), messageCaptor.capture());
            assertThat(messageCaptor.getValue().getContentId()).isEqualTo("content-id-123");
        }

        @Test
        void whenFinalValidationFails_setsStatusToFailedAndReturnsError() {
            // ARRANGE
            when(contentRepository.findByIdAndUserId(anyString(), anyString())).thenReturn(Mono.just(sampleDraft));
            doThrow(new ValidationException("Missing required field 'postTitle'")).when(templateValidator).validate(anyString(), any(), eq(true));
            when(contentRepository.save(any(Content.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
            
            // ACT
            Mono<Content> resultMono = contentService.submitForGeneration("content-id-123", "user-id-abc");

            // ASSERT
            StepVerifier.create(resultMono)
                .expectError(IllegalArgumentException.class)
                .verify();

            // Verify that the content was saved with FAILED status BEFORE the error was thrown
            ArgumentCaptor<Content> contentCaptor = ArgumentCaptor.forClass(Content.class);
            verify(contentRepository, times(1)).save(contentCaptor.capture());
            assertThat(contentCaptor.getValue().getStatus()).isEqualTo(ContentStatus.FAILED);
            assertThat(contentCaptor.getValue().getErrorMessage()).contains("Missing required field 'postTitle'");
        }
    }

    @Nested
    @DisplayName("processStatusUpdate Tests")
    class ProcessStatusUpdateTests {
        @Test
        void whenStatusIsCompleted_updatesContentCorrectly() {
            // ARRANGE
            // do nothing when processorService.processUploadJob is called
            doNothing().when(processorService).processUploadJob(any());
            GenerationResultV1 generationResult = new GenerationResultV1("contend-id-123", ContentStatus.COMPLETED, null, null);
            when(contentRepository.findById(anyString())).thenReturn(Mono.just(sampleDraft));
            when(contentRepository.save(any(Content.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
            
            // ACT
            Mono<Content> resultMono = contentService.processGenerationResult(generationResult);
            
            // ASSERT
            StepVerifier.create(resultMono)
                .assertNext(content -> {
                    assertThat(content.getStatus()).isEqualTo(ContentStatus.COMPLETED);
                    assertThat(content.getErrorMessage()).isNull();
                })
                .verifyComplete();
        }

        @Test
        void whenContentIsInTerminalState_ignoresUpdate() {
            // ARRANGE
            sampleDraft.setStatus(ContentStatus.COMPLETED); // Set a terminal state
            GenerationResultV1 generationResult = new GenerationResultV1("contend-id-123", ContentStatus.COMPLETED, null, null);
            when(contentRepository.findById(anyString())).thenReturn(Mono.just(sampleDraft));

            // ACT
            Mono<Content> resultMono = contentService.processGenerationResult(generationResult);

            // ASSERT
            StepVerifier.create(resultMono)
                .assertNext(content -> {
                    // Verify that the status DID NOT change
                    assertThat(content.getStatus()).isEqualTo(ContentStatus.COMPLETED);
                })
                .verifyComplete();
            
            // Verify that save was NEVER called
            verify(contentRepository, never()).save(any(Content.class));
        }
    }
}
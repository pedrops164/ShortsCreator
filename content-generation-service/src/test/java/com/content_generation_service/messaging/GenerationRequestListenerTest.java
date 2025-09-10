package com.content_generation_service.messaging;

import com.content_generation_service.config.AppProperties;
import com.content_generation_service.generation.orchestrator.RedditStoryOrchestrator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shortscreator.shared.dto.GeneratedVideoDetailsV1;
import com.shortscreator.shared.dto.GenerationRequestV1;
import com.shortscreator.shared.dto.GenerationResultV1;
import com.shortscreator.shared.enums.ContentStatus;
import com.shortscreator.shared.validation.TemplateValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import org.springframework.test.context.ActiveProfiles;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("dev") // Ensure it loads application-dev.yml with API keys etc.
class GenerationRequestListenerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private TemplateValidator templateValidator;
    @Mock
    private AppProperties appProperties;
    // We also need to mock the nested objects that will be called
    @Mock
    private AppProperties.RabbitMQ rabbitMQMock;
    @Mock
    private AppProperties.RoutingKeys routingKeysMock;
    @Mock
    private RedditStoryOrchestrator redditStoryOrchestrator;
    @Mock
    private GenerationResultDispatcher generationResultDispatcher; // Mock the dispatcher

    @InjectMocks // Creates an instance of the listener and injects the mocks into it
    private GenerationRequestListener listener;

    private GenerationRequestV1 sampleRequest;

    @BeforeEach
    void setUp() {
        // Create a consistent test input
        sampleRequest = new GenerationRequestV1("content-id-123", "user-id-456", "reddit_story_v1", null);
        // Stub the behavior of the AppProperties mock
        // This prevents further NullPointerExceptions on chained calls like .getRabbitmq().getExchange()
        //when(appProperties.getRabbitmq()).thenReturn(rabbitMQMock);
        //when(rabbitMQMock.getExchange()).thenReturn("test-exchange"); // Provide a dummy exchange name
        //when(rabbitMQMock.getRoutingKeys()).thenReturn(routingKeysMock);
        //when(routingKeysMock.getStatusUpdate()).thenReturn("update.status"); // Provide a dummy routing key
    }

    @Test
    void whenValidatorSucceeds_thenGenerationProceeds() {
        // Instruct the mock validator to do nothing (succeed)
        doNothing().when(templateValidator).validate(anyString(), any(), eq(true));
        GeneratedVideoDetailsV1 videoDetails = new GeneratedVideoDetailsV1();
        when(redditStoryOrchestrator.generate(any(), any(), any()))
            .thenReturn(videoDetails);

        listener.handleGenerationRequest(sampleRequest);

        // ASSERT: Verify that the "success" path was taken.
        // For example, capture the argument and check for a COMPLETED status.
        ArgumentCaptor<GenerationResultV1> captor = ArgumentCaptor.forClass(GenerationResultV1.class);
        verify(templateValidator).validate(anyString(), any(), eq(true));
        verify(generationResultDispatcher).dispatch(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ContentStatus.COMPLETED);
        assertThat(captor.getValue().getGeneratedVideoDetails()).isEqualTo(videoDetails);
        verify(redditStoryOrchestrator, times(1)).generate(any(), any(), any());
    }

    @Test
    void whenValidationFails_thenSendsFailedStatusUpdate() {
        // This test simulates a validation failure
        doThrow(new TemplateValidator.ValidationException("This is my fake exception"))
            .when(templateValidator).validate(anyString(), any(), anyBoolean());

        // When the listener processes the request, it should catch the exception
        listener.handleGenerationRequest(sampleRequest);

        // This part will likely still fail, but the console output above will tell us why.
        ArgumentCaptor<GenerationResultV1> statusUpdateCaptor = ArgumentCaptor.forClass(GenerationResultV1.class);
        verify(generationResultDispatcher, times(1)).dispatch(statusUpdateCaptor.capture());
        assertThat(statusUpdateCaptor.getValue().getStatus()).isEqualTo(ContentStatus.FAILED);
    }
}
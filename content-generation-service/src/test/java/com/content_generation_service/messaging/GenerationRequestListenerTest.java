package com.content_generation_service.messaging;

import com.content_generation_service.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shortscreator.shared.dto.GenerationRequestV1;
import com.shortscreator.shared.dto.StatusUpdateV1;
import com.shortscreator.shared.enums.ContentStatus;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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

    @InjectMocks // Creates an instance of the listener and injects the mocks into it
    private GenerationRequestListener listener;

    private GenerationRequestV1 sampleRequest;

    @BeforeEach
    void setUp() {
        // Create a consistent test input
        sampleRequest = new GenerationRequestV1("content-id-123", "user-id-456", "reddit_story_v1", null);
        // Stub the behavior of the AppProperties mock
        // This prevents further NullPointerExceptions on chained calls like .getRabbitmq().getExchange()
        when(appProperties.getRabbitmq()).thenReturn(rabbitMQMock);
        when(rabbitMQMock.getExchange()).thenReturn("test-exchange"); // Provide a dummy exchange name
        when(rabbitMQMock.getRoutingKeys()).thenReturn(routingKeysMock);
        when(routingKeysMock.getStatusUpdate()).thenReturn("update.status"); // Provide a dummy routing key
    }

    @Test
    void whenValidatorSucceeds_thenGenerationProceeds() {
        // Instruct the mock validator to do nothing (succeed)
        doNothing().when(templateValidator).validate(anyString(), any(), eq(true));
        
        listener.handleGenerationRequest(sampleRequest);

        // ASSERT: Verify that the "success" path was taken.
        // For example, capture the argument and check for a COMPLETED status.
        ArgumentCaptor<StatusUpdateV1> captor = ArgumentCaptor.forClass(StatusUpdateV1.class);
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ContentStatus.COMPLETED);
    }

    @Test
    void whenValidationFails_thenSendsFailedStatusUpdate() {
        // This test simulates a validation failure
        doThrow(new TemplateValidator.ValidationException("This is my fake exception"))
            .when(templateValidator).validate(anyString(), any(), anyBoolean());

        // When the listener processes the request, it should catch the exception
        listener.handleGenerationRequest(sampleRequest);

        // This part will likely still fail, but the console output above will tell us why.
        ArgumentCaptor<StatusUpdateV1> statusUpdateCaptor = ArgumentCaptor.forClass(StatusUpdateV1.class);
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), statusUpdateCaptor.capture());
        assertThat(statusUpdateCaptor.getValue().getStatus()).isEqualTo(ContentStatus.FAILED);
    }

    @Test
    void whenProcessingThrowsException_thenSendsFailedStatusUpdate() throws InterruptedException {
        // --- ARRANGE ---

        // 1. We must ensure validation SUCCEEDS for this test case.
        // We tell the mock validator to do nothing when called.
        doNothing().when(templateValidator).validate(anyString(), any(), eq(true));

        // Let's mock the "COMPLETED" message send call to throw an exception
        doThrow(new RuntimeException("Failed to connect to broker"))
            .when(rabbitTemplate).convertAndSend(anyString(), eq("update.status.completed"), any(StatusUpdateV1.class));

        // --- ACT ---
        // Call the method under test
        listener.handleGenerationRequest(sampleRequest);

        // --- ASSERT ---
        // We expect `rabbitTemplate.convertAndSend` to be called TWICE:
        // 1. The first time, it tries to send the "COMPLETED" message (and we mocked it to fail).
        // 2. The second time, inside the catch block, it sends the "FAILED" message.

        ArgumentCaptor<StatusUpdateV1> statusUpdateCaptor = ArgumentCaptor.forClass(StatusUpdateV1.class);
        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);

        // Verify that the method was called a total of 2 times.
        verify(rabbitTemplate, times(2)).convertAndSend(
            anyString(),
            routingKeyCaptor.capture(),
            statusUpdateCaptor.capture()
        );

        // Now we check the details of the SECOND invocation, which should be the FAILED message.
        String finalRoutingKey = routingKeyCaptor.getAllValues().get(1);
        StatusUpdateV1 finalUpdate = statusUpdateCaptor.getAllValues().get(1);

        assertThat(finalRoutingKey).isEqualTo("update.status.failed");
        assertThat(finalUpdate.getStatus()).isEqualTo(ContentStatus.FAILED);
        assertThat(finalUpdate.getContentId()).isEqualTo("content-id-123");
        
        // Check that the error message contains the message from our mocked exception
        assertThat(finalUpdate.getErrorMessage()).contains("Failed to connect to broker");
    }
}
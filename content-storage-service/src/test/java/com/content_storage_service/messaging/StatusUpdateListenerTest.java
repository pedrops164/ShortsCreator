package com.content_storage_service.messaging;

import com.content_storage_service.config.AppProperties;
import com.content_storage_service.service.ContentService;
import com.shortscreator.shared.dto.StatusUpdateV1;
import com.shortscreator.shared.enums.ContentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatusUpdateListenerTest {

    @Mock
    private ContentService contentService;
    
    @Mock
    private AppProperties appProperties;

    @InjectMocks // Creates an instance of StatusUpdateListener and injects the mocks
    private StatusUpdateListener listener;

    @Test
    void whenHandleStatusUpdateIsCalled_thenContentServiceIsInvoked() {
        // ARRANGE
        // Create a sample DTO that we expect to receive from the queue
        StatusUpdateV1 updateDto = new StatusUpdateV1("content-id-123", ContentStatus.COMPLETED, null, null);

        // When the contentService.processStatusUpdate method is called, we tell it to return an empty Mono.
        // We don't care about the result, only that the method was called.
        when(contentService.processStatusUpdate(any(StatusUpdateV1.class))).thenReturn(Mono.empty());

        // ACT
        // Directly invoke the method on our listener instance
        listener.handleStatusUpdate(updateDto);

        // ASSERT
        // Verify that the processStatusUpdate method on our contentService mock was called
        // exactly 1 time with the exact DTO we created.
        verify(contentService, times(1)).processStatusUpdate(updateDto);
    }
}

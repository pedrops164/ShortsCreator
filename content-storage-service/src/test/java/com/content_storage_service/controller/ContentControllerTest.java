package com.content_storage_service.controller;

import com.content_storage_service.dto.ContentCreationRequest;
import com.content_storage_service.model.Content;
import com.content_storage_service.service.ContentSecurity;
import com.content_storage_service.service.ContentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shortscreator.shared.enums.ContentStatus;
import com.shortscreator.shared.enums.ContentType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import java.time.Duration;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.security.core.Authentication;

import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

// Exclude the MongoDB auto-configurations for this web-layer-only test
@WebFluxTest(controllers = ContentController.class)
class ContentControllerTest {

    @Autowired
    private WebTestClient webTestClient; // A non-blocking client for testing REST endpoints

    @MockitoBean
    private ContentService contentService;

    @Autowired
    private ContentSecurity contentSecurity;

    @Autowired
    private ObjectMapper objectMapper;

    // --- NEW: EXPLICIT TEST CONFIGURATION ---
    @TestConfiguration
    static class ControllerTestConfig {
        /**
         * This explicitly creates a mock of ContentSecurity and registers it
         * in the Spring Context with the specific bean name "contentSecurity".
         * This is a more robust way to ensure the @PreAuthorize expression can find it.
         */
        @Bean("contentSecurity")
        public ContentSecurity contentSecurity() {
            return mock(ContentSecurity.class);
        }

        // we are receiving the No bean named 'mongoMappingContext' available error, so we need to add it manually
        @Bean
        public MongoMappingContext mongoMappingContext() {
            return new MongoMappingContext();
        }
    }

    @BeforeEach
    void setUp() {
        // This takes the existing auto-configured client and "mutates" it,
        // returning a new instance with a longer response timeout.
        this.webTestClient = this.webTestClient.mutate()
            .responseTimeout(Duration.ofMinutes(5)) // Set a long timeout for debugging
            .build();
    }

    @Test
    @WithMockUser(username = "test-user") // Mocks the Principal object for this test
    void createContentDraft_whenRequestIsValid_returns201Created() throws Exception {
        // ARRANGE
        ContentCreationRequest request = new ContentCreationRequest();
        request.setTemplateId("reddit_story_v1");
        request.setContentType(ContentType.REDDIT_STORY);
        request.setTemplateParams(objectMapper.createObjectNode().put("title", "A Test Title"));

        // Mock the service layer to return a successfully created Content object
        Content mockResponse = new Content();
        mockResponse.setId("new-content-id");
        mockResponse.setUserId("test-user");
        mockResponse.setStatus(ContentStatus.DRAFT);
        when(contentService.createDraft(any(), any(), any(), any())).thenReturn(Mono.just(mockResponse));
        
        // ACT & ASSERT
        webTestClient
            .mutateWith(csrf()) // Add a valid mock CSRF token to the request
            .post().uri("/api/v1/content/drafts")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(request))
            .exchange()
            .expectStatus().isCreated() // Check for HTTP 201
            .expectBody()
            .jsonPath("$.id").isEqualTo("new-content-id")
            .jsonPath("$.status").isEqualTo("DRAFT");
    }

    @Test
    @WithMockUser(username = "test-user")
    void submitContentForGeneration_whenDraftExists_returns202Accepted() {
        // ARRANGE
        String contentId = "draft-to-submit-123";
        String userId = "test-user";

        // Telling our mock bean: "When your isOwner method is called, just return true."
        when(contentSecurity.isOwner(anyString(), any(Authentication.class)))
            .thenReturn(Mono.just(true));

        Content mockResponse = new Content();
        mockResponse.setId(contentId);
        mockResponse.setUserId(userId);
        mockResponse.setStatus(ContentStatus.PROCESSING); // The service returns the object after setting status
        when(contentService.submitForGeneration(contentId, userId)).thenReturn(Mono.just(mockResponse));
        
        // ACT & ASSERT
        webTestClient
            .mutateWith(csrf()) // Add a valid mock CSRF token to the request
            .post().uri("/api/v1/content/{contentId}/generate", contentId)
            .exchange()
            .expectStatus().isAccepted() // Check for HTTP 202
            .expectBody()
            .jsonPath("$.id").isEqualTo(contentId)
            .jsonPath("$.status").isEqualTo("PROCESSING");
    }
}
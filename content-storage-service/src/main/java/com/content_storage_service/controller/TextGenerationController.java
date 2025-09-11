package com.content_storage_service.controller;
import org.springframework.web.bind.annotation.*;

import com.content_storage_service.dto.GenerateTextRequest;
import com.content_storage_service.dto.GeneratedContentResponse;
import com.content_storage_service.service.TextGenerationService;

import jakarta.validation.Valid;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/generate")
public class TextGenerationController {

    private final TextGenerationService textGenerationService;

    public TextGenerationController(TextGenerationService textGenerationService) {
        this.textGenerationService = textGenerationService;
    }

    @PostMapping("/text")
    public Mono<GeneratedContentResponse> generateText(@Valid @RequestBody GenerateTextRequest request, @RequestHeader("X-User-ID") String userId) {
        return textGenerationService.generateContent(request, userId);
    }
}
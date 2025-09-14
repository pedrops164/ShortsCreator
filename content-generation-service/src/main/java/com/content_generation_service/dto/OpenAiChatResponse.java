package com.content_generation_service.dto;

import java.util.List;

public record OpenAiChatResponse(
    List<Choice> choices
) {
    public record Choice(
        ResponseMessage message
    ) {}

    public record ResponseMessage(
        String content
    ) {}
}
package com.bfrost.backend.chat.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record SendMessageRequest(
        UUID conversationId,
        @NotBlank String body
) {}

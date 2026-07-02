package com.bfrost.backend.event.dto;

import com.bfrost.backend.event.EventFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateEventRequest(
        @NotBlank String      title,
        String                description,
        @NotNull EventFormat  format,
        String                location,
        @NotNull Instant      startTime,
        Instant               endTime,
        Integer               maxMembers,
        String                coverImageUrl
) {}
package com.bfrost.backend.event.dto;

import com.bfrost.backend.event.EventFormat;
import jakarta.validation.Valid;

import java.time.Instant;
import java.util.List;

public record UpdateEventRequest(
        String       title,
        String       description,
        EventFormat  format,
        String       location,
        Instant      startTime,
        Instant      endTime,
        Integer      maxMembers,
        String       coverImageUrl,
        @Valid List<RegistrationDto.QuestionInput> questions
) {}

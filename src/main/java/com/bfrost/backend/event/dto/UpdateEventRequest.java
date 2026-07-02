package com.bfrost.backend.event.dto;

import com.bfrost.backend.event.EventFormat;

import java.time.Instant;

public record UpdateEventRequest(
        String       title,
        String       description,
        EventFormat  format,
        String       location,
        Instant      startTime,
        Instant      endTime,
        Integer      maxMembers,
        String       coverImageUrl
) {}

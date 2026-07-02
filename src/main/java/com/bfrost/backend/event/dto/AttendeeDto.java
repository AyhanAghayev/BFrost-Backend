package com.bfrost.backend.event.dto;

import com.bfrost.backend.event.Rsvp;

import java.time.Instant;
import java.util.UUID;

public record AttendeeDto(
        UUID    userId,
        String  username,
        String  displayName,
        String  profilePictureUrl,
        Instant respondedAt
) {
    public static AttendeeDto from(Rsvp r) {
        return new AttendeeDto(
                r.getUser().getId(),
                r.getUser().getUsername(),
                r.getUser().getDisplayName(),
                r.getUser().getProfilePictureUrl(),
                r.getRespondedAt()
        );
    }
}
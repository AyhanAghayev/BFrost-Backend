package com.bfrost.backend.event.dto;

import com.bfrost.backend.event.ClubEvent;

import java.time.Instant;
import java.util.UUID;

public record EventDto(
        UUID    id,
        UUID    clubId,
        String  clubSlug,
        String  clubName,
        String  title,
        String  description,
        String  coverImageUrl,
        String  format,
        String  location,
        Instant startTime,
        Instant endTime,
        Integer maxMembers,
        UUID    createdBy,
        long    attendingCount,
        String  currentUserRsvp,
        Instant createdAt
) {
    public static EventDto from(ClubEvent e, long attending, String rsvp) {
        return new EventDto(
                e.getId(), e.getClub().getId(), e.getClub().getSlug(), e.getClub().getName(),
                e.getTitle(), e.getDescription(),
                e.getCoverImageUrl(), e.getFormat().name(), e.getLocation(),
                e.getStartTime(), e.getEndTime(), e.getMaxMembers(),
                e.getCreatedBy().getId(), attending, rsvp, e.getCreatedAt()
        );
    }
}
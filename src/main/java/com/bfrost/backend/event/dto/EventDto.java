package com.bfrost.backend.event.dto;

import com.bfrost.backend.event.ClubEvent;
import com.bfrost.backend.event.EventQuestion;

import java.time.Instant;
import java.util.List;
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
        long    waitlistCount,
        String  currentUserRsvp,
        List<RegistrationDto.QuestionDto> questions,
        Instant createdAt
) {
    public static EventDto from(ClubEvent e, long attending, long waitlist, String rsvp,
                                List<EventQuestion> questions) {
        return new EventDto(
                e.getId(), e.getClub().getId(), e.getClub().getSlug(), e.getClub().getName(),
                e.getTitle(), e.getDescription(),
                e.getCoverImageUrl(), e.getFormat().name(), e.getLocation(),
                e.getStartTime(), e.getEndTime(), e.getMaxMembers(),
                e.getCreatedBy().getId(), attending, waitlist, rsvp,
                questions.stream().map(RegistrationDto.QuestionDto::from).toList(),
                e.getCreatedAt()
        );
    }
}

package com.bfrost.backend.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RsvpRepository extends JpaRepository<Rsvp, UUID> {
    Optional<Rsvp> findByEventIdAndUserId(UUID eventId, UUID userId);
    long countByEventIdAndStatus(UUID eventId, RsvpStatus status);
    List<Rsvp> findByEventIdAndStatusOrderByRespondedAtAsc(UUID eventId, RsvpStatus status);
    // Oldest waitlisted registrant, to promote when a spot frees.
    Optional<Rsvp> findFirstByEventIdAndStatusOrderByRespondedAtAsc(UUID eventId, RsvpStatus status);
}
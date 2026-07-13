package com.bfrost.backend.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventQuestionRepository extends JpaRepository<EventQuestion, UUID> {
    List<EventQuestion> findByEventIdOrderByPosition(UUID eventId);
    void deleteByEventId(UUID eventId);
}

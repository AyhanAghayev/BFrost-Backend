package com.bfrost.backend.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface EventRepository extends JpaRepository<ClubEvent, UUID> {
    List<ClubEvent> findByClubIdOrderByStartTimeAsc(UUID clubId);
    List<ClubEvent> findByClubIdAndStartTimeAfterOrderByStartTimeAsc(UUID clubId, Instant after);

    @Query(value = """
        SELECT e.* FROM events e
        JOIN memberships m ON m.club_id = e.club_id
        WHERE m.user_id = :userId
          AND e.start_time > NOW()
        ORDER BY e.start_time ASC
        LIMIT 50
        """, nativeQuery = true)
    List<ClubEvent> findUpcomingEventsForUser(UUID userId);
}
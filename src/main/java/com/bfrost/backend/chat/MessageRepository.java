package com.bfrost.backend.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {


    @Query(value = """
        SELECT m.* FROM messages m
        LEFT JOIN conversation_clearances cc
            ON cc.user_id = :userId AND cc.conversation_id = m.conversation_id
        WHERE m.conversation_id = :conversationId
          AND m.created_at > COALESCE(cc.cleared_at, '-infinity'::timestamp)
          AND (CAST(:cursor AS timestamp) IS NULL
               OR m.created_at < CAST(:cursor AS timestamp)
               OR (m.created_at = CAST(:cursor AS timestamp) AND m.id < CAST(:cursorId AS uuid)))
        ORDER BY m.created_at DESC, m.id DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Message> findVisibleMessages(UUID conversationId, UUID userId, Instant cursor, UUID cursorId, int limit);

    @Query(value = """
        SELECT COUNT(*) FROM messages m
        JOIN conversations c ON c.id = m.conversation_id
        LEFT JOIN conversation_reads r
            ON r.user_id = :userId AND r.conversation_id = m.conversation_id
        LEFT JOIN conversation_clearances cc
            ON cc.user_id = :userId AND cc.conversation_id = m.conversation_id
        WHERE (c.user_a_id = :userId OR c.user_b_id = :userId)
          AND m.sender_id <> :userId
          AND m.created_at > GREATEST(
                COALESCE(r.last_read_at, '-infinity'::timestamp),
                COALESCE(cc.cleared_at, '-infinity'::timestamp))
        """, nativeQuery = true)
    long countUnreadForUser(UUID userId);

    @Query(value = """
        SELECT COUNT(*) FROM messages m
        LEFT JOIN conversation_reads r
            ON r.user_id = :userId AND r.conversation_id = m.conversation_id
        LEFT JOIN conversation_clearances cc
            ON cc.user_id = :userId AND cc.conversation_id = m.conversation_id
        WHERE m.conversation_id = :conversationId
          AND m.sender_id <> :userId
          AND m.created_at > GREATEST(
                COALESCE(r.last_read_at, '-infinity'::timestamp),
                COALESCE(cc.cleared_at, '-infinity'::timestamp))
        """, nativeQuery = true)
    long countUnreadInConversation(UUID conversationId, UUID userId);
}

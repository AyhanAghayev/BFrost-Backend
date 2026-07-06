package com.bfrost.backend.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface ConversationClearanceRepository extends JpaRepository<ConversationClearance, ConversationClearanceId> {

    @Modifying
    @Query(value = """
        INSERT INTO conversation_clearances(user_id, conversation_id, cleared_at)
        VALUES (:userId, :conversationId, NOW())
        ON CONFLICT (user_id, conversation_id) DO UPDATE SET cleared_at = NOW()
        """, nativeQuery = true)
    void upsertClearance(UUID userId, UUID conversationId);
}
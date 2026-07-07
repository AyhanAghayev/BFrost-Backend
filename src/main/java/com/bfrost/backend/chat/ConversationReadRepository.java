package com.bfrost.backend.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface ConversationReadRepository extends JpaRepository<ConversationRead, ConversationReadId> {

    @Modifying
    @Query(value = """
        INSERT INTO conversation_reads(user_id, conversation_id, last_read_at)
        VALUES (:userId, :conversationId, NOW())
        ON CONFLICT (user_id, conversation_id) DO UPDATE SET last_read_at = NOW()
        """, nativeQuery = true)
    void upsertRead(UUID userId, UUID conversationId);
}

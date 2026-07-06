package com.bfrost.backend.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    @Query("SELECT c FROM Conversation c WHERE (c.userA.id = :userA AND c.userB.id = :userB) OR (c.userA.id = :userB AND c.userB.id = :userA)")
    Optional<Conversation> findBetween(UUID userA, UUID userB);

    @Query("SELECT c FROM Conversation c WHERE c.userA.id = :userId OR c.userB.id = :userId ORDER BY c.createdAt DESC")
    List<Conversation> findAllForUser(UUID userId);
}
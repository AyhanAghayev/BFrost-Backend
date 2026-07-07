package com.bfrost.backend.chat;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversation_clearances")
@IdClass(ConversationClearanceId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationClearance {
    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Id
    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "cleared_at", nullable = false)
    private Instant clearedAt;

    @PrePersist
    @PreUpdate
    void prePersist() { this.clearedAt = Instant.now(); }
}

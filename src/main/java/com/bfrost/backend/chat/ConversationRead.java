package com.bfrost.backend.chat;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversation_reads")
@IdClass(ConversationReadId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationRead {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Id
    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "last_read_at", nullable = false)
    private Instant lastReadAt;

    @PrePersist
    @PreUpdate
    void touch() { this.lastReadAt = Instant.now(); }
}
package com.bfrost.backend.chat.dto;

import com.bfrost.backend.chat.Message;

import java.time.Instant;
import java.util.UUID;

public record MessageDto(
        UUID    id,
        UUID    conversationId,
        UUID    senderId,
        String  senderUsername,
        String  senderDisplayName,
        String  senderPictureUrl,
        String  body,
        Instant createdAt
) {
    public static MessageDto from(Message m) {
        return new MessageDto(
                m.getId(),
                m.getConversation().getId(),
                m.getSender().getId(),
                m.getSender().getUsername(),
                m.getSender().getDisplayName(),
                m.getSender().getProfilePictureUrl(),
                m.getBody(),
                m.getCreatedAt()
        );
    }
}

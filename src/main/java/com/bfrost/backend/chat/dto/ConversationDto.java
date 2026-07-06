package com.bfrost.backend.chat.dto;

import com.bfrost.backend.chat.Conversation;

import java.time.Instant;
import java.util.UUID;

public record ConversationDto(
        UUID    id,
        UUID    otherUserId,
        String  otherUsername,
        String  otherDisplayName,
        String  otherPictureUrl,
        long    unreadCount,
        Instant createdAt
) {
    public static ConversationDto from(Conversation c, UUID currentUserId, long unreadCount) {
        boolean currentIsA = c.getUserA().getId().equals(currentUserId);
        var other = currentIsA ? c.getUserB() : c.getUserA();
        return new ConversationDto(
                c.getId(),
                other.getId(),
                other.getUsername(),
                other.getDisplayName(),
                other.getProfilePictureUrl(),
                unreadCount,
                c.getCreatedAt()
        );
    }
}
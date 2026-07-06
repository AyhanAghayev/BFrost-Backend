package com.bfrost.backend.notification.dto;

import com.bfrost.backend.notification.Notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationDto(
        UUID    id,
        UUID actorId,
        String  actorUsername,
        String  actorDisplayName,
        String  actorPictureUrl,
        String  type,
        UUID    targetId,
        String  targetType,
        String  message,
        boolean read,
        Instant createdAt
) {
    public static NotificationDto from(Notification n) {
        return new NotificationDto(
                n.getId(),
                n.getActor().getId(),
                n.getActor().getUsername(),
                n.getActor().getDisplayName(),
                n.getActor().getProfilePictureUrl(),
                n.getType().name(),
                n.getTargetId(),
                n.getTargetType(),
                n.getMessage(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}

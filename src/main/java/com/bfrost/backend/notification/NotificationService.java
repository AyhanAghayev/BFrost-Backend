package com.bfrost.backend.notification;

import com.bfrost.backend.notification.dto.NotificationDto;
import com.bfrost.backend.user.User;
import com.bfrost.backend.user.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<NotificationDto> getNotifications(UUID userId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.markAllReadForUser(userId);
    }

    @Transactional
    public void push(UUID recipientId, UUID actorId, NotificationType type, UUID targetId, String targetType, String message) {
        if (recipientId.equals(actorId)) return; // No self-notifications
        User recipient = userRepository.findById(recipientId).orElse(null);
        if (recipient == null || !wantsNotification(recipient, type)) return; // Respect recipient's preferences
        User actor = userRepository.getReferenceById(actorId);
        notificationRepository.save(Notification.builder()
                .recipient(recipient)
                .actor(actor)
                .type(type)
                .targetId(targetId)
                .targetType(targetType)
                .message(message)
                .build());
    }

    private boolean wantsNotification(User u, NotificationType type) {
        return switch (type) {
            case FOLLOW       -> u.isNotifyFollow();
            case LIKE         -> u.isNotifyLike();
            case COMMENT      -> u.isNotifyComment();
            case JOIN_REQUEST -> u.isNotifyJoinRequest();
            case CLUB_APPROVED, CLUB_REJECTED -> true;
        };
    }
}

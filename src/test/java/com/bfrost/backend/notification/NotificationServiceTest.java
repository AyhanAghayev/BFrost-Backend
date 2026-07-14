package com.bfrost.backend.notification;

import com.bfrost.backend.notification.dto.NotificationDto;
import com.bfrost.backend.user.User;
import com.bfrost.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, userRepository);
    }

    @Test
    void getNotificationsMapsRepositoryResultsToDtos() {
        UUID userId = UUID.randomUUID();
        User actor = User.builder().id(UUID.randomUUID()).username("actor").displayName("Actor").build();
        User recipient = User.builder().id(userId).build();
        Notification n = Notification.builder().id(UUID.randomUUID()).recipient(recipient).actor(actor)
                .type(NotificationType.FOLLOW).message("started following you").build();
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(n));

        List<NotificationDto> result = notificationService.getNotifications(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).actorUsername()).isEqualTo("actor");
        assertThat(result.get(0).type()).isEqualTo("FOLLOW");
    }

    @Test
    void getUnreadCountDelegatesToRepository() {
        UUID userId = UUID.randomUUID();
        when(notificationRepository.countByRecipientIdAndReadFalse(userId)).thenReturn(3L);

        assertThat(notificationService.getUnreadCount(userId)).isEqualTo(3L);
    }

    @Test
    void markAllReadDelegatesToRepository() {
        UUID userId = UUID.randomUUID();

        notificationService.markAllRead(userId);

        verify(notificationRepository).markAllReadForUser(userId);
    }

    @Test
    void pushSkipsSelfNotification() {
        UUID userId = UUID.randomUUID();

        notificationService.push(userId, userId, NotificationType.LIKE, UUID.randomUUID(), "post", "liked your post");

        verifyNoInteractions(userRepository, notificationRepository);
    }

    @Test
    void pushSkipsWhenRecipientNotFound() {
        UUID recipientId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        when(userRepository.findById(recipientId)).thenReturn(Optional.empty());

        notificationService.push(recipientId, actorId, NotificationType.LIKE, UUID.randomUUID(), "post", "liked your post");

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void pushSkipsWhenRecipientDisabledThatNotificationType() {
        UUID recipientId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        User recipient = User.builder().id(recipientId).notifyFollow(false).build();
        when(userRepository.findById(recipientId)).thenReturn(Optional.of(recipient));

        notificationService.push(recipientId, actorId, NotificationType.FOLLOW, actorId, "user", "started following you");

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void pushSavesNotificationWhenRecipientWantsIt() {
        UUID recipientId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        User recipient = User.builder().id(recipientId).notifyFollow(true).build();
        User actor = User.builder().id(actorId).build();
        when(userRepository.findById(recipientId)).thenReturn(Optional.of(recipient));
        when(userRepository.getReferenceById(actorId)).thenReturn(actor);

        notificationService.push(recipientId, actorId, NotificationType.FOLLOW, actorId, "user", "started following you");

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void pushAlwaysSavesClubApprovedNotificationRegardlessOfPreferences() {
        UUID recipientId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        User recipient = User.builder().id(recipientId).build();
        User actor = User.builder().id(actorId).build();
        when(userRepository.findById(recipientId)).thenReturn(Optional.of(recipient));
        when(userRepository.getReferenceById(actorId)).thenReturn(actor);

        notificationService.push(recipientId, actorId, NotificationType.CLUB_APPROVED, UUID.randomUUID(), "club", "your club was approved");

        verify(notificationRepository).save(any(Notification.class));
    }
}

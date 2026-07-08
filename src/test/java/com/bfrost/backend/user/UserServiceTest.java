package com.bfrost.backend.user;


import com.bfrost.backend.common.exception.ConflictException;
import com.bfrost.backend.common.exception.ForbiddenException;
import com.bfrost.backend.common.exception.ResourceNotFoundException;
import com.bfrost.backend.notification.NotificationService;
import com.bfrost.backend.user.dto.UserProfileDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock private FollowRepository followRepository;
    @Mock private NotificationService notificationService;
    @Mock private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, followRepository, notificationService, passwordEncoder);
    }

    private User aUser() {
        return User.builder().id(UUID.randomUUID()).username("alice").email("alice@example.com")
                .passwordHash("hashed").displayName("Alice").build();
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() {
        User user = aUser();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(user.getId(), "wrong", "newpassword123"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void changePasswordRejectsShortNewPassword() {
        User user = aUser();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current", "hashed")).thenReturn(true);

        assertThatThrownBy(() -> userService.changePassword(user.getId(), "current", "short"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changePasswordSucceeds() {
        User user = aUser();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current", "hashed")).thenReturn(true);
        when(passwordEncoder.encode("newpassword123")).thenReturn("new-hashed");

        userService.changePassword(user.getId(), "current", "newpassword123");

        assertThat(user.getPasswordHash()).isEqualTo("new-hashed");
    }

    @Test
    void changeEmailRejectsDuplicateEmail() {
        User user = aUser();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current", "hashed")).thenReturn(true);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.changeEmail(user.getId(), "new@example.com", "current"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void changeEmailRejectsInvalidFormat() {
        User user = aUser();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current", "hashed")).thenReturn(true);

        assertThatThrownBy(() -> userService.changeEmail(user.getId(), "not-an-email", "current"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changeEmailSucceedsAndResetsVerification() {
        User user = aUser();
        user.setEmailVerified(true);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current", "hashed")).thenReturn(true);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        userService.changeEmail(user.getId(), "new@example.com", "current");

        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.isEmailVerified()).isFalse();
    }

    @Test
    void followRejectsSelfFollow() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> userService.follow(id, id)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void followRejectsDuplicateFollow() {
        UUID follower = UUID.randomUUID();
        UUID followee = UUID.randomUUID();
        when(followRepository.existsByFollowerIdAndFolloweeId(follower, followee)).thenReturn(true);

        assertThatThrownBy(() -> userService.follow(follower, followee)).isInstanceOf(ConflictException.class);
    }

    @Test
    void unfollowRejectsWhenNotFollowing() {
        UUID follower = UUID.randomUUID();
        UUID followee = UUID.randomUUID();
        when(followRepository.findByFollowerIdAndFolloweeId(follower, followee)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.unfollow(follower, followee))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateProfileRejectsUpdatingAnotherUsersProfile() {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        assertThatThrownBy(() -> userService.updateProfile(owner, other, null))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getProfileOnlyIncludesEmailForSelf() {
        User user = aUser();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(followRepository.countByFolloweeId(user.getId())).thenReturn(0L);
        when(followRepository.countByFollowerId(user.getId())).thenReturn(0L);

        UserProfileDto asStranger = userService.getProfile("alice", UUID.randomUUID());
        UserProfileDto asSelf = userService.getProfile("alice", user.getId());

        assertThat(asStranger.email()).isNull();
        assertThat(asSelf.email()).isEqualTo("alice@example.com");
    }
}


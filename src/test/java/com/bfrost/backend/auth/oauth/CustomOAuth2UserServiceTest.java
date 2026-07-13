package com.bfrost.backend.auth.oauth;

import com.bfrost.backend.user.RegistrationStatus;
import com.bfrost.backend.user.User;
import com.bfrost.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock private UserRepository userRepository;

    private CustomOAuth2UserService service;

    @BeforeEach
    void setUp() {
        service = new CustomOAuth2UserService(userRepository);
    }

    private Map<String, Object> googleAttributes(String sub, String email, boolean emailVerified) {
        return Map.of(
                "sub", sub,
                "email", email,
                "email_verified", emailVerified,
                "name", "Alice Example",
                "picture", "https://example.com/pic.jpg"
        );
    }

    @Test
    void createsNewUserWhenNoGoogleIdOrEmailMatch() {
        when(userRepository.findByGoogleId("google-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = service.resolveUser(googleAttributes("google-1", "alice@example.com", true));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("alice");
        assertThat(saved.getEmail()).isEqualTo("alice@example.com");
        assertThat(saved.getGoogleId()).isEqualTo("google-1");
        assertThat(saved.isEmailVerified()).isTrue();
        assertThat(saved.getPasswordHash()).isNull();
        assertThat(saved.getRegistrationStatus()).isEqualTo(RegistrationStatus.PENDING);
        assertThat(saved.getDisplayName()).isEqualTo("Alice Example");
        assertThat(result).isSameAs(saved);
    }

    @Test
    void linksExistingVerifiedEmailUserByGoogleId() {
        User existing = User.builder().id(UUID.randomUUID()).username("alice")
                .email("alice@example.com").displayName("Alice").build();
        when(userRepository.findByGoogleId("google-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        User result = service.resolveUser(googleAttributes("google-1", "alice@example.com", true));

        assertThat(result).isSameAs(existing);
        assertThat(existing.getGoogleId()).isEqualTo("google-1");
        assertThat(existing.getRegistrationStatus()).isEqualTo(RegistrationStatus.COMPLETE);
        verify(userRepository).save(existing);
    }

    @Test
    void returnsExistingUserWhenGoogleIdAlreadyLinked() {
        User existing = User.builder().id(UUID.randomUUID()).username("alice")
                .email("alice@example.com").googleId("google-1").displayName("Alice").build();
        when(userRepository.findByGoogleId("google-1")).thenReturn(Optional.of(existing));

        User result = service.resolveUser(googleAttributes("google-1", "alice@example.com", true));

        assertThat(result).isSameAs(existing);
        verify(userRepository, never()).save(any());
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void doesNotLinkByEmailWhenGoogleReportsEmailUnverified() {
        when(userRepository.findByGoogleId("google-1")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = service.resolveUser(googleAttributes("google-1", "alice@example.com", false));

        verify(userRepository, never()).findByEmail(any());
        assertThat(result.getGoogleId()).isEqualTo("google-1");
        assertThat(result.getUsername()).isEqualTo("alice");
        assertThat(result.getRegistrationStatus()).isEqualTo(RegistrationStatus.PENDING);
    }
}

package com.bfrost.backend.auth;

import com.bfrost.backend.auth.dto.LoginRequest;
import com.bfrost.backend.auth.dto.RegisterRequest;
import com.bfrost.backend.common.exception.ConflictException;
import com.bfrost.backend.user.RegistrationStatus;
import com.bfrost.backend.user.User;
import com.bfrost.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PendingRegistrationTokenRepository pendingRegistrationTokenRepository;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, refreshTokenRepository, pendingRegistrationTokenRepository, jwtService, passwordEncoder);
        ReflectionTestUtils.setField(authService, "refreshTokenExpiryMs", 604_800_000L);
        ReflectionTestUtils.setField(authService, "registrationTokenExpiryMs", 900_000L);
    }

    @Test
    void registerCreatesUserAndIssuesTokens() {
        RegisterRequest req = new RegisterRequest("alice", "alice@example.com", "password123", "Alice");
        when(userRepository.existsByEmail(req.email())).thenReturn(false);
        when(userRepository.existsByUsername(req.username())).thenReturn(false);
        when(passwordEncoder.encode(req.password())).thenReturn("hashed");
        when(jwtService.generateAccessToken(any(), eq("alice"))).thenReturn("access-token");

        AuthResult result = authService.register(req);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.response().username()).isEqualTo("alice");
        assertThat(result.refreshToken()).isNotBlank();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("alice@example.com");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest req = new RegisterRequest("alice", "alice@example.com", "password123", "Alice");
        when(userRepository.existsByEmail(req.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req)).isInstanceOf(ConflictException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerRejectsDuplicateUsername() {
        RegisterRequest req = new RegisterRequest("alice", "alice@example.com", "password123", "Alice");
        when(userRepository.existsByEmail(req.email())).thenReturn(false);
        when(userRepository.existsByUsername(req.username())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req)).isInstanceOf(ConflictException.class);
    }

    @Test
    void loginSucceedsWithCorrectCredentials() {
        User user = User.builder().id(UUID.randomUUID()).username("alice").email("alice@example.com")
            .passwordHash("hashed").displayName("Alice").build();
        LoginRequest req = new LoginRequest("alice@example.com", "password123");
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(req.password(), "hashed")).thenReturn(true);
        when(jwtService.generateAccessToken(user.getId(), "alice")).thenReturn("access-token");

        AuthResult result = authService.login(req);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(user.getLastLoginAt()).isNotNull();
    }

    @Test
    void loginRejectsUnknownEmail() {
        LoginRequest req = new LoginRequest("nobody@example.com", "password123");
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req)).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void loginRejectsWrongPassword() {
        User user = User.builder().id(UUID.randomUUID()).username("alice").email("alice@example.com")
            .passwordHash("hashed").displayName("Alice").build();
        LoginRequest req = new LoginRequest("alice@example.com", "wrong-password");
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(req.password(), "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req)).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void loginRejectsPendingOAuthUserWithoutPassword() {
        User user = User.builder().id(UUID.randomUUID()).username("alice").email("alice@example.com")
            .passwordHash(null).registrationStatus(RegistrationStatus.PENDING).displayName("Alice").build();
        LoginRequest req = new LoginRequest("alice@example.com", "whatever");
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(req)).isInstanceOf(BadCredentialsException.class);
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void issuePendingRegistrationTokenReplacesAnyExistingTokenForUser() {
        User user = User.builder().id(UUID.randomUUID()).username("alice").displayName("Alice").build();

        String token = authService.issuePendingRegistrationToken(user);

        assertThat(token).isNotBlank();
        verify(pendingRegistrationTokenRepository).deleteAllByUserId(user.getId());
        ArgumentCaptor<PendingRegistrationToken> captor = ArgumentCaptor.forClass(PendingRegistrationToken.class);
        verify(pendingRegistrationTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getToken()).isEqualTo(token);
        assertThat(captor.getValue().getUser()).isEqualTo(user);
    }

    @Test
    void completeRegistrationHashesPasswordFinalizesUserAndIssuesTokens() {
        User user = User.builder().id(UUID.randomUUID()).username("alice").email("alice@example.com")
            .registrationStatus(RegistrationStatus.PENDING).displayName("Alice").build();
        PendingRegistrationToken prt = PendingRegistrationToken.builder().token("reg-token").user(user)
            .expiresAt(Instant.now().plusSeconds(3600)).build();
        when(pendingRegistrationTokenRepository.findByToken("reg-token")).thenReturn(Optional.of(prt));
        when(passwordEncoder.encode("newpassword")).thenReturn("hashed");
        when(jwtService.generateAccessToken(user.getId(), "alice")).thenReturn("access-token");

        AuthResult result = authService.completeRegistration("reg-token", "newpassword");

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(user.getPasswordHash()).isEqualTo("hashed");
        assertThat(user.getRegistrationStatus()).isEqualTo(RegistrationStatus.COMPLETE);
        verify(pendingRegistrationTokenRepository).deleteAllByUserId(user.getId());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void completeRegistrationRejectsUnknownToken() {
        when(pendingRegistrationTokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.completeRegistration("bad-token", "newpassword"))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void completeRegistrationRejectsExpiredTokenAndDeletesIt() {
        User user = User.builder().id(UUID.randomUUID()).username("alice").registrationStatus(RegistrationStatus.PENDING).build();
        PendingRegistrationToken prt = PendingRegistrationToken.builder().token("expired").user(user)
            .expiresAt(Instant.now().minusSeconds(3600)).build();
        when(pendingRegistrationTokenRepository.findByToken("expired")).thenReturn(Optional.of(prt));

        assertThatThrownBy(() -> authService.completeRegistration("expired", "newpassword"))
            .isInstanceOf(BadCredentialsException.class);
        verify(pendingRegistrationTokenRepository).delete(prt);
    }

    @Test
    void refreshReusesTokenWithoutRotation() {
        User user = User.builder().id(UUID.randomUUID()).username("alice").displayName("Alice").build();
        RefreshToken rt = RefreshToken.builder().token("refresh-value").user(user)
            .expiresAt(Instant.now().plusSeconds(3600)).build();
        when(refreshTokenRepository.findByToken("refresh-value")).thenReturn(Optional.of(rt));
        when(jwtService.generateAccessToken(user.getId(), "alice")).thenReturn("new-access-token");

        AuthResult result = authService.refresh("refresh-value");

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-value");
        verify(refreshTokenRepository, never()).delete(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refreshRejectsExpiredTokenAndDeletesIt() {
        User user = User.builder().id(UUID.randomUUID()).username("alice").build();
        RefreshToken rt = RefreshToken.builder().token("expired").user(user)
            .expiresAt(Instant.now().minusSeconds(3600)).build();
        when(refreshTokenRepository.findByToken("expired")).thenReturn(Optional.of(rt));

        assertThatThrownBy(() -> authService.refresh("expired")).isInstanceOf(BadCredentialsException.class);
        verify(refreshTokenRepository).delete(rt);
    }

    @Test
    void refreshRejectsUnknownToken() {
        when(refreshTokenRepository.findByToken("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("unknown")).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void logoutDeletesAllRefreshTokensForUser() {
        UUID userId = UUID.randomUUID();
        authService.logout(userId);
        verify(refreshTokenRepository).deleteAllByUserId(userId);
    }
}

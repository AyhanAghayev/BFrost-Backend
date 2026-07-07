package com.bfrost.backend.auth;

import com.bfrost.backend.auth.dto.AuthResponse;
import com.bfrost.backend.auth.dto.LoginRequest;
import com.bfrost.backend.auth.dto.RegisterRequest;
import com.bfrost.backend.common.exception.ConflictException;
import com.bfrost.backend.user.User;
import com.bfrost.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Value("${bfrost.jwt.refresh-token-expiry-ms}")
    private long refreshTokenExpiryMs;

    @Transactional
    public AuthResult register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ConflictException("Email already in use");
        }
        if (userRepository.existsByUsername(req.username())) {
            throw new ConflictException("Username already taken");
        }
        User user = User.builder()
                .username(req.username())
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .displayName(req.displayName())
                .build();
        userRepository.save(user);
        return buildResult(user);
    }

    @Transactional
    public AuthResult login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        user.setLastLoginAt(Instant.now());
        return buildResult(user);
    }

    @Transactional
    public AuthResult refresh(String refreshTokenValue) {
        RefreshToken rt = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        if (rt.isExpired()) {
            refreshTokenRepository.delete(rt);
            throw new BadCredentialsException("Refresh token expired");
        }

        User user = rt.getUser();
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername());
        return new AuthResult(
                new AuthResponse(user.getId(), user.getUsername(), user.getDisplayName()),
                accessToken,
                refreshTokenValue
        );
    }

    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.deleteAllByUserId(userId);
    }

    private AuthResult buildResult(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername());
        String refreshTokenValue = UUID.randomUUID().toString();
        RefreshToken rt = RefreshToken.builder()
                .user(user)
                .token(refreshTokenValue)
                .expiresAt(Instant.now().plusMillis(refreshTokenExpiryMs))
                .build();
        refreshTokenRepository.save(rt);
        return new AuthResult(
                new AuthResponse(user.getId(), user.getUsername(), user.getDisplayName()),
                accessToken,
                refreshTokenValue
        );
    }
}
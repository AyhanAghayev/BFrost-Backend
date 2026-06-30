package com.bfrost.backend.auth;

import com.bfrost.backend.auth.dto.AuthResponse;
import com.bfrost.backend.auth.dto.LoginRequest;
import com.bfrost.backend.auth.dto.RegisterRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${bfrost.jwt.refresh-token-expiry-ms}")
    private long refreshTokenExpiryMs;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req, HttpServletResponse response) {
        AuthResult result = authService.register(req);
        setRefreshCookie(response, result.refreshToken());
        return result.response();
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req, HttpServletResponse response) {
        AuthResult result = authService.login(req);
        setRefreshCookie(response, result.refreshToken());
        return result.response();
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String token = extractCookie(request, "refreshToken");
        AuthResult result = authService.refresh(token);
        setRefreshCookie(response, result.refreshToken());
        return result.response();
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response,
                       @AuthenticationPrincipal BFrostUserDetails principal) {
        if (principal != null) {
            authService.logout(principal.userId());
        }
        clearRefreshCookie(response);
    }

    private void setRefreshCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("refreshToken", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge((int) (refreshTokenExpiryMs / 1000));
        cookie.setSecure(false);
        response.addCookie(cookie);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
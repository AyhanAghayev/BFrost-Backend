package com.bfrost.backend.auth;

import com.bfrost.backend.auth.dto.AuthResponse;
import com.bfrost.backend.auth.dto.CompleteRegistrationRequest;
import com.bfrost.backend.auth.dto.LoginRequest;
import com.bfrost.backend.auth.dto.RegisterRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService cookieService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req, HttpServletResponse response) {
        AuthResult result = authService.register(req);
        cookieService.setAccessTokenCookie(response, result.accessToken());
        cookieService.setRefreshCookie(response, result.refreshToken());
        return result.response();
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req, HttpServletResponse response) {
        AuthResult result = authService.login(req);
        cookieService.setAccessTokenCookie(response, result.accessToken());
        cookieService.setRefreshCookie(response, result.refreshToken());
        return result.response();
    }

    @PostMapping("/complete-registration")
    public AuthResponse completeRegistration(@Valid @RequestBody CompleteRegistrationRequest req, HttpServletResponse response) {
        AuthResult result = authService.completeRegistration(req.token(), req.password());
        cookieService.setAccessTokenCookie(response, result.accessToken());
        cookieService.setRefreshCookie(response, result.refreshToken());
        return result.response();
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String token = extractCookie(request, "refreshToken");
        AuthResult result = authService.refresh(token);
        cookieService.setAccessTokenCookie(response, result.accessToken());
        cookieService.setRefreshCookie(response, result.refreshToken());
        return result.response();
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response,
                       @AuthenticationPrincipal BFrostUserDetails principal) {
        if (principal != null) {
            authService.logout(principal.userId());
        }
        cookieService.clearAccessTokenCookie(response);
        cookieService.clearRefreshCookie(response);
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

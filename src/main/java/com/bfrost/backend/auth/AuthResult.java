package com.bfrost.backend.auth;

import com.bfrost.backend.auth.dto.AuthResponse;

public record AuthResult(AuthResponse response, String accessToken, String refreshToken) {}
package com.bfrost.backend.auth.dto;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        UUID   userId,
        String username,
        String displayName
) {}
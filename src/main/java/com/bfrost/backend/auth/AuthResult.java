package com.bfrost.backend.auth;

import com.bfrost.backend.auth.dto.AuthResponse;

record AuthResult(AuthResponse response, String refreshToken) {}
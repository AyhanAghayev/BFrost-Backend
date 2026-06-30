package com.bfrost.backend.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @Size(min = 3, max = 30)
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username may only contain letters, numbers and underscores")
    String username,
    @Size(max = 100) String displayName,
    @Size(max = 500) String bio,
    String profilePictureUrl,
    String backgroundUrl,
    @Size(max = 255) String university,
    @Size(max = 255) String department
) {}

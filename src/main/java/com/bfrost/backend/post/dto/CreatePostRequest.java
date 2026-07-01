package com.bfrost.backend.post.dto;

import com.bfrost.backend.post.PostType;
import com.bfrost.backend.post.TargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreatePostRequest(
        @NotNull TargetType targetType,
        @NotNull UUID targetId,
        @NotNull PostType postType,
        @Size(max = 255) String title,
        @NotBlank String body,
        String mediaUrl,
        String linkUrl,
        String channel
) {}

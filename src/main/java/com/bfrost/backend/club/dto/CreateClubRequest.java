package com.bfrost.backend.club.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateClubRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 100) String slug,
        @Size(max = 2000)          String description,
        @NotBlank @Size(max = 50)  String category,
        boolean                    isPublic,
        Set<String> tags
) {}

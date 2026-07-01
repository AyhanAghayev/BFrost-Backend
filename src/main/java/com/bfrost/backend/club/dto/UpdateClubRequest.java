package com.bfrost.backend.club.dto;

import jakarta.validation.constraints.Size;

import java.util.Set;

public record UpdateClubRequest(
        @Size(max = 100)  String      name,
        @Size(max = 100)  String      slug,
        @Size(max = 2000) String      description,
        @Size(max = 50)   String      category,
        Boolean                       isPublic,
        Set<String> tags,
        String                        coverImageUrl,
        String                        logoUrl
) {}

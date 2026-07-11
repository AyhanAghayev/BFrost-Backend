package com.bfrost.backend.wiki.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWikiRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 300)           String summary,
        @NotBlank @Size(max = 20000) String body,
        boolean                    featured
) {}

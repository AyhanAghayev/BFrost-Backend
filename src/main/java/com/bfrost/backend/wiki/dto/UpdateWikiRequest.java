package com.bfrost.backend.wiki.dto;

import jakarta.validation.constraints.Size;

public record UpdateWikiRequest(
        @Size(max = 255)   String  title,
        @Size(max = 300)   String  summary,
        @Size(max = 20000) String  body,
        Boolean                    featured
) {}

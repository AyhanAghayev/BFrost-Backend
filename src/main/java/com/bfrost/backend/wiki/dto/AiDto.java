package com.bfrost.backend.wiki.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AiDto {
    private AiDto() {}

    public record DraftRequest(@NotBlank @Size(max = 200) String topic) {}
    public record SummarizeRequest(@NotBlank @Size(max = 20000) String body) {}

    public record DraftResponse(String summary, String body) {}
    public record SummaryResponse(String summary) {}
    public record StatusResponse(boolean enabled) {}
}

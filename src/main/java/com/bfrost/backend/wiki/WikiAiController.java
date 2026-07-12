package com.bfrost.backend.wiki;

import com.bfrost.backend.auth.BFrostUserDetails;
import com.bfrost.backend.wiki.dto.AiDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WikiAiController {

    private final WikiAiService wikiAiService;

    // lets the frontend know when whether to hide or show the ai buttons.
    @GetMapping("/wiki/ai/status")
    public AiDto.StatusResponse status() {
        return new AiDto.StatusResponse(wikiAiService.isEnabled());
    }

    @PostMapping("/clubs/{clubId}/wiki/ai-draft")
    public AiDto.DraftResponse draft(@PathVariable UUID clubId,
                                     @Valid @RequestBody AiDto.DraftRequest req,
                                     @AuthenticationPrincipal BFrostUserDetails principal) {
        return wikiAiService.draft(clubId, principal.userId(), req.topic());
    }

    @PostMapping("/clubs/{clubId}/wiki/ai-summarize")
    public AiDto.SummaryResponse summarize(@PathVariable UUID clubId,
                                           @Valid @RequestBody AiDto.SummarizeRequest req,
                                           @AuthenticationPrincipal BFrostUserDetails principal) {
        return wikiAiService.summarize(clubId, principal.userId(), req.body());
    }
}

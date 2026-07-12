package com.bfrost.backend.wiki;

import com.bfrost.backend.auth.BFrostUserDetails;
import com.bfrost.backend.wiki.dto.CreateWikiRequest;
import com.bfrost.backend.wiki.dto.UpdateWikiRequest;
import com.bfrost.backend.wiki.dto.WikiArticleDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WikiController {

    private final WikiService wikiService;

    private static UUID userId(BFrostUserDetails p) {
        return p != null ? p.userId() : null;
    }

    @GetMapping("/wiki/feed")
    public List<WikiArticleDto> feed(@AuthenticationPrincipal BFrostUserDetails principal) {
        return wikiService.getFeed(principal.userId());
    }

    @GetMapping("/clubs/{clubSlug}/wiki")
    public List<WikiArticleDto> byClub(@PathVariable String clubSlug,
                                       @AuthenticationPrincipal BFrostUserDetails principal) {
        return wikiService.listByClub(clubSlug, userId(principal));
    }

    @GetMapping("/wiki/{articleId}")
    public WikiArticleDto get(@PathVariable UUID articleId,
                              @AuthenticationPrincipal BFrostUserDetails principal) {
        return wikiService.getById(articleId, userId(principal));
    }

    @GetMapping("/users/{userId}/wiki")
    public List<WikiArticleDto> byAuthor(@PathVariable("userId") UUID authorId,
                                         @AuthenticationPrincipal BFrostUserDetails principal) {
        return wikiService.getByAuthor(authorId, userId(principal));
    }

    @PostMapping("/clubs/{clubId}/wiki")
    @ResponseStatus(HttpStatus.CREATED)
    public WikiArticleDto create(@PathVariable UUID clubId,
                                 @Valid @RequestBody CreateWikiRequest req,
                                 @AuthenticationPrincipal BFrostUserDetails principal) {
        return wikiService.create(clubId, req, principal.userId());
    }

    @PatchMapping("/wiki/{articleId}")
    public WikiArticleDto update(@PathVariable UUID articleId,
                                 @Valid @RequestBody UpdateWikiRequest req,
                                 @AuthenticationPrincipal BFrostUserDetails principal) {
        return wikiService.update(articleId, req, principal.userId());
    }

    @DeleteMapping("/wiki/{articleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID articleId,
                       @AuthenticationPrincipal BFrostUserDetails principal) {
        wikiService.delete(articleId, principal.userId());
    }
}

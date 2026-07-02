package com.bfrost.backend.post;

import com.bfrost.backend.auth.BFrostUserDetails;
import com.bfrost.backend.club.ClubRepository;
import com.bfrost.backend.common.CursorPage;
import com.bfrost.backend.common.exception.ResourceNotFoundException;
import com.bfrost.backend.post.dto.CreatePostRequest;
import com.bfrost.backend.post.dto.PostDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final ClubRepository clubRepository;

    @GetMapping("/feed")
    public CursorPage<PostDto> feed(@RequestParam(required = false) String cursor,
                                    @RequestParam(defaultValue = "20") int size,
                                    @AuthenticationPrincipal BFrostUserDetails principal) {
        return postService.getFeed(principal.userId(), cursor, size);
    }

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public PostDto create(@Valid @RequestBody CreatePostRequest req,
                          @AuthenticationPrincipal BFrostUserDetails principal) {
        return postService.create(req, principal.userId());
    }

    @GetMapping("/posts/{postId}")
    public PostDto getPost(@PathVariable UUID postId,
                           @AuthenticationPrincipal BFrostUserDetails principal) {
        return postService.getById(postId, principal != null ? principal.userId() : null);
    }

    @DeleteMapping("/posts/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(@PathVariable UUID postId,
                           @AuthenticationPrincipal BFrostUserDetails principal) {
        postService.deletePost(postId, principal.userId());
    }

    @PostMapping("/posts/{postId}/react")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void react(@PathVariable UUID postId,
                      @RequestParam ReactionType type,
                      @AuthenticationPrincipal BFrostUserDetails principal) {
        postService.react(postId, principal.userId(), type);
    }

    @GetMapping("/users/{userId}/posts")
    public CursorPage<PostDto> getUserPosts(@PathVariable UUID userId,
                                            @RequestParam(required = false) String cursor,
                                            @RequestParam(defaultValue = "20") int size,
                                            @AuthenticationPrincipal BFrostUserDetails principal) {
        return postService.getTargetPosts(TargetType.USER_PAGE, userId, principal != null ? principal.userId() : null, cursor, size);
    }

    @GetMapping("/clubs/{clubSlug}/posts")
    public CursorPage<PostDto> getClubPosts(@PathVariable String clubSlug,
                                            @RequestParam(required = false) String cursor,
                                            @RequestParam(defaultValue = "20") int size,
                                            @AuthenticationPrincipal BFrostUserDetails principal) {
        UUID clubId = clubRepository.findBySlug(clubSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Club not found: " + clubSlug))
                .getId();
        return postService.getTargetPosts(TargetType.CLUB_PAGE, clubId, principal != null ? principal.userId() : null, cursor, size);
    }

    record CommentBody(@NotBlank String body) {}
}

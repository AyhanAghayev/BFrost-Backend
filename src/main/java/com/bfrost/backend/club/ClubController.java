package com.bfrost.backend.club;

import com.bfrost.backend.auth.BFrostUserDetails;
import com.bfrost.backend.club.dto.ClubDto;
import com.bfrost.backend.club.dto.CreateClubRequest;
import com.bfrost.backend.club.dto.MemberDto;
import com.bfrost.backend.club.dto.UpdateClubRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clubs")
@RequiredArgsConstructor
public class ClubController {

    private final ClubService clubService;

    @GetMapping
    public List<ClubDto> listAll(@AuthenticationPrincipal BFrostUserDetails principal) {
        return clubService.listAll(principal != null ? principal.userId() : null);
    }

    @GetMapping("/{slug}")
    public ClubDto getClub(@PathVariable String slug,
                           @AuthenticationPrincipal BFrostUserDetails principal) {
        return clubService.getClub(slug, principal != null ? principal.userId() : null);
    }

    @GetMapping("/search")
    public List<ClubDto> search(@RequestParam String q,
                                @AuthenticationPrincipal BFrostUserDetails principal) {
        return clubService.search(q, principal != null ? principal.userId() : null);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClubDto create(@Valid @RequestBody CreateClubRequest req,
                          @AuthenticationPrincipal BFrostUserDetails principal) {
        return clubService.create(req, principal.userId());
    }

    @PatchMapping("/{slug}")
    public ClubDto update(@PathVariable String slug,
                          @Valid @RequestBody UpdateClubRequest req,
                          @AuthenticationPrincipal BFrostUserDetails principal) {
        return clubService.update(slug, principal.userId(), req);
    }



    @DeleteMapping("/{clubId}/join")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable UUID clubId,
                      @AuthenticationPrincipal BFrostUserDetails principal) {
        clubService.leave(clubId, principal.userId());
    }

    @GetMapping("/{clubId}/members")
    public List<MemberDto> members(@PathVariable UUID clubId) {
        return clubService.getMembers(clubId);
    }

    @GetMapping("/{clubId}/requests")
    public List<com.bfrost.backend.club.dto.JoinRequestDto> requests(@PathVariable UUID clubId,
                                                             @AuthenticationPrincipal BFrostUserDetails principal) {
        return clubService.getPendingRequests(clubId, principal.userId());
    }

    public record SetRoleRequest(String role) {}

    @PatchMapping("/{clubId}/members/{userId}/role")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setMemberRole(@PathVariable UUID clubId, @PathVariable UUID userId,
                              @RequestBody SetRoleRequest req,
                              @AuthenticationPrincipal BFrostUserDetails principal) {
        clubService.setMemberRole(clubId, userId, principal.userId(), MemberRole.valueOf(req.role()));
    }

    @DeleteMapping("/{clubId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable UUID clubId, @PathVariable UUID userId,
                             @AuthenticationPrincipal BFrostUserDetails principal) {
        clubService.removeMember(clubId, userId, principal.userId());
    }

    @PostMapping("/{clubId}/transfer/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void transferOwnership(@PathVariable UUID clubId, @PathVariable UUID userId,
                                  @AuthenticationPrincipal BFrostUserDetails principal) {
        clubService.transferOwnership(clubId, userId, principal.userId());
    }

    @PostMapping("/{clubId}/requests/{requestId}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approveRequest(@PathVariable UUID clubId, @PathVariable UUID requestId,
                               @AuthenticationPrincipal BFrostUserDetails principal) {
        clubService.approveRequest(clubId, requestId, principal.userId());
    }

    @PostMapping("/{clubId}/requests/{requestId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectRequest(@PathVariable UUID clubId, @PathVariable UUID requestId,
                              @AuthenticationPrincipal BFrostUserDetails principal) {
        clubService.rejectRequest(clubId, requestId, principal.userId());
    }
}

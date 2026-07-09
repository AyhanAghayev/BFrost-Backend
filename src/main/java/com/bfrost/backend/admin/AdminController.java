package com.bfrost.backend.admin;

import com.bfrost.backend.auth.BFrostUserDetails;
import com.bfrost.backend.club.ClubRepository;
import com.bfrost.backend.club.ClubService;
import com.bfrost.backend.club.ClubStatus;
import com.bfrost.backend.club.MembershipRepository;
import com.bfrost.backend.club.dto.ClubDto;
import com.bfrost.backend.event.EventRepository;
import com.bfrost.backend.post.PostRepository;
import com.bfrost.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ClubService clubService;
    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final EventRepository eventRepository;
    private final MembershipRepository membershipRepository;

    @GetMapping("/stats")
    public AdminStatsDto stats() {
        return new AdminStatsDto(
                userRepository.count(),
                clubRepository.count(),
                clubRepository.countByStatus(ClubStatus.APPROVED),
                clubRepository.countByStatus(ClubStatus.PENDING),
                postRepository.count(),
                eventRepository.count(),
                membershipRepository.count()
        );
    }

    @GetMapping("/clubs/pending")
    public List<ClubDto> pendingClubs(@AuthenticationPrincipal BFrostUserDetails principal) {
        return clubService.getPendingClubs(principal.userId());
    }

    @PostMapping("/clubs/{clubId}/approve")
    public ClubDto approve(@PathVariable UUID clubId,
                           @AuthenticationPrincipal BFrostUserDetails principal) {
        return clubService.approveClub(clubId, principal.userId());
    }

    @DeleteMapping("/clubs/{clubId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@PathVariable UUID clubId,
                       @AuthenticationPrincipal BFrostUserDetails principal) {
        clubService.rejectClub(clubId, principal.userId());
    }
}

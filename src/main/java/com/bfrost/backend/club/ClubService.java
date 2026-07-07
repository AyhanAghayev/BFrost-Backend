package com.bfrost.backend.club;



import com.bfrost.backend.club.dto.*;
import com.bfrost.backend.common.exception.ConflictException;
import com.bfrost.backend.common.exception.ForbiddenException;
import com.bfrost.backend.common.exception.ResourceNotFoundException;
import com.bfrost.backend.notification.NotificationService;
import com.bfrost.backend.notification.NotificationType;
import com.bfrost.backend.user.User;
import com.bfrost.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClubService {

    private final ClubRepository clubRepository;
    private final MembershipRepository membershipRepository;
    private final MembershipRequestRepository membershipRequestRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public ClubDto getClub(String identifier, UUID currentUserId) {
        return buildDto(resolveClub(identifier), currentUserId);
    }

    private Club resolveClub(String identifier) {
        try {
            return clubRepository.findById(UUID.fromString(identifier))
                    .orElseThrow(() -> new ResourceNotFoundException("Club not found: " + identifier));
        } catch (IllegalArgumentException ignored) {
            return clubRepository.findBySlug(identifier)
                    .orElseThrow(() -> new ResourceNotFoundException("Club not found: " + identifier));
        }
    }

    @Transactional
    public ClubDto update(String identifier, UUID currentUserId, UpdateClubRequest req) {
        Club club = resolveClub(identifier);
        requireModerator(club.getId(), currentUserId);
        if (req.name() != null) club.setName(req.name());
        if (req.description() != null) club.setDescription(req.description());
        if (req.category() != null) club.setCategory(req.category());
        if (req.isPublic() != null) club.setPublic(req.isPublic());
        if (req.tags() != null) club.setTags(req.tags());
        if (req.coverImageUrl() != null) club.setCoverImageUrl(req.coverImageUrl());
        if (req.logoUrl() != null) club.setLogoUrl(req.logoUrl());
        if (req.slug() != null && !req.slug().equals(club.getSlug())) {
            if (clubRepository.existsBySlug(req.slug())) {
                throw new ConflictException("Slug already taken: " + req.slug());
            }
            club.setSlug(req.slug());
        }
        clubRepository.save(club);
        return buildDto(club, currentUserId);
    }

    @Transactional(readOnly = true)
    public List<ClubDto> listAll(UUID currentUserId) {
        return clubRepository.findAllOrderByMemberCount(50).stream()
                .map(c -> buildDto(c, currentUserId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClubDto> search(String query, UUID currentUserId) {
        return clubRepository.searchByText(query, 20).stream()
                .map(c -> buildDto(c, currentUserId))
                .toList();
    }

    @Transactional
    public ClubDto create(CreateClubRequest req, UUID ownerId) {
        if (clubRepository.existsBySlug(req.slug())) {
            throw new ConflictException("Slug already taken: " + req.slug());
        }
        User owner = userRepository.getReferenceById(ownerId);
        Club club = Club.builder()
                .name(req.name())
                .slug(req.slug())
                .description(req.description())
                .owner(owner)
                .isPublic(req.isPublic())
                .category(req.category())
                .tags(req.tags() != null ? req.tags() : new java.util.HashSet<>())
                .build();
        clubRepository.save(club);
        Membership ownerMembership = Membership.builder()
                .club(club)
                .user(owner)
                .role(MemberRole.OWNER)
                .build();
        membershipRepository.save(ownerMembership);
        return buildDto(club, ownerId);
    }

    @Transactional
    public JoinResultDto join(UUID clubId, UUID userId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club not found"));
        if (membershipRepository.existsByClubIdAndUserId(clubId, userId)) {
            throw new ConflictException("Already a member");
        }
        User user = userRepository.getReferenceById(userId);
        if (club.isPublic()) {
            membershipRepository.save(Membership.builder().club(club).user(user).build());
            return new JoinResultDto("JOINED");
        }
        MembershipRequest request = membershipRequestRepository.findByClubIdAndUserId(clubId, userId)
                .orElseGet(() -> MembershipRequest.builder().club(club).user(user).build());
        if (request.getStatus() == RequestStatus.PENDING && request.getId() != null) {
            throw new ConflictException("Join request already pending");
        }
        request.setStatus(RequestStatus.PENDING);
        membershipRequestRepository.save(request);
        notificationService.push(club.getOwner().getId(), userId,
                NotificationType.JOIN_REQUEST, club.getId(), "club",
                "requested to join " + club.getName());
        return new JoinResultDto("REQUESTED");
    }

    @Transactional(readOnly = true)
    public List<JoinRequestDto> getPendingRequests(UUID clubId, UUID currentUserId) {
        requireModerator(clubId, currentUserId);
        return membershipRequestRepository.findByClubIdAndStatus(clubId, RequestStatus.PENDING).stream()
                .map(JoinRequestDto::from)
                .toList();
    }

    @Transactional
    public void leave(UUID clubId, UUID userId) {
        Membership m = membershipRepository.findByClubIdAndUserId(clubId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Not a member"));
        if (m.getRole() == MemberRole.OWNER) throw new ForbiddenException("Owner cannot leave; transfer ownership first");
        membershipRepository.delete(m);
    }

    @Transactional
    public void approveRequest(UUID clubId, UUID requestId, UUID moderatorId) {
        requireModerator(clubId, moderatorId);
        MembershipRequest req = membershipRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        if (!req.getClub().getId().equals(clubId))
            throw new ForbiddenException("Request does not belong to this club");
        if (membershipRepository.existsByClubIdAndUserId(clubId, req.getUser().getId()))
            throw new ConflictException("User is already a member");
        req.setStatus(RequestStatus.APPROVED);
        membershipRepository.save(Membership.builder().club(req.getClub()).user(req.getUser()).build());
    }

    @Transactional
    public void rejectRequest(UUID clubId, UUID requestId, UUID moderatorId) {
        requireModerator(clubId, moderatorId);
        MembershipRequest req = membershipRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        if (!req.getClub().getId().equals(clubId))
            throw new ForbiddenException("Request does not belong to this club");
        req.setStatus(RequestStatus.REJECTED);
    }

    @Transactional(readOnly = true)
    public List<MemberDto> getMembers(UUID clubId) {
        return membershipRepository.findByClubId(clubId).stream()
                .map(MemberDto::from)
                .toList();
    }

    @Transactional
    public void setMemberRole(UUID clubId, UUID targetUserId, UUID actorId, MemberRole role) {
        requireOwner(clubId, actorId);
        if (role == MemberRole.OWNER) throw new ForbiddenException("Use transfer ownership to assign an owner");
        Membership m = membershipRepository.findByClubIdAndUserId(clubId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User is not a member"));
        if (m.getRole() == MemberRole.OWNER) throw new ForbiddenException("Cannot change the owner's role");
        m.setRole(role);
        membershipRepository.save(m);
    }

    @Transactional
    public void removeMember(UUID clubId, UUID targetUserId, UUID actorId) {
        Membership actor = membershipRepository.findByClubIdAndUserId(clubId, actorId)
                .orElseThrow(() -> new ForbiddenException("Not a club member"));
        if (actor.getRole() == MemberRole.MEMBER) throw new ForbiddenException("Moderator or owner required");
        Membership target = membershipRepository.findByClubIdAndUserId(clubId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User is not a member"));
        if (target.getRole() == MemberRole.OWNER) throw new ForbiddenException("Cannot remove the owner");
        if (actor.getRole() == MemberRole.MODERATOR && target.getRole() == MemberRole.MODERATOR) {
            throw new ForbiddenException("Only the owner can remove a moderator");
        }
        membershipRepository.delete(target);
    }

    @Transactional
    public void transferOwnership(UUID clubId, UUID newOwnerId, UUID actorId) {
        requireOwner(clubId, actorId);
        Membership current = membershipRepository.findByClubIdAndUserId(clubId, actorId)
                .orElseThrow(() -> new ForbiddenException("Not a club member"));
        Membership next = membershipRepository.findByClubIdAndUserId(clubId, newOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("New owner must be a club member"));
        current.setRole(MemberRole.MODERATOR);
        next.setRole(MemberRole.OWNER);
        membershipRepository.save(current);
        membershipRepository.save(next);
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club not found"));
        club.setOwner(userRepository.getReferenceById(newOwnerId));
        clubRepository.save(club);
    }

    private void requireModerator(UUID clubId, UUID userId) {
        Membership m = membershipRepository.findByClubIdAndUserId(clubId, userId)
                .orElseThrow(() -> new ForbiddenException("Not a club member"));
        if (m.getRole() == MemberRole.MEMBER) throw new ForbiddenException("Moderator or owner required");
    }

    private void requireOwner(UUID clubId, UUID userId) {
        Membership m = membershipRepository.findByClubIdAndUserId(clubId, userId)
                .orElseThrow(() -> new ForbiddenException("Not a club member"));
        if (m.getRole() != MemberRole.OWNER) throw new ForbiddenException("Only the club owner can do this");
    }

    private ClubDto buildDto(Club club, UUID currentUserId) {
        long memberCount = membershipRepository.countByClubId(club.getId());
        boolean isMember = false;
        String role = null;
        boolean hasPendingRequest = false;
        if (currentUserId != null) {
            var membership = membershipRepository.findByClubIdAndUserId(club.getId(), currentUserId);
            isMember = membership.isPresent();
            role = membership.map(m -> m.getRole().name()).orElse(null);
            if (!isMember) {
                hasPendingRequest = membershipRequestRepository
                        .existsByClubIdAndUserIdAndStatus(club.getId(), currentUserId, RequestStatus.PENDING);
            }
        }
        return ClubDto.from(club, memberCount, isMember, role, hasPendingRequest);
    }
}

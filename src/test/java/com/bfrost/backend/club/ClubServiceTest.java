package com.bfrost.backend.club;

import com.bfrost.backend.club.dto.CreateClubRequest;
import com.bfrost.backend.club.dto.JoinResultDto;
import com.bfrost.backend.common.exception.ConflictException;
import com.bfrost.backend.common.exception.ForbiddenException;
import com.bfrost.backend.common.exception.ResourceNotFoundException;
import com.bfrost.backend.notification.NotificationService;
import com.bfrost.backend.user.User;
import com.bfrost.backend.user.UserRepository;
import com.bfrost.backend.wiki.WikiArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClubServiceTest {

    @Mock
    private ClubRepository clubRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private MembershipRequestRepository membershipRequestRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private WikiArticleRepository wikiArticleRepository;

    private ClubService clubService;

    @BeforeEach
    void setUp() {
        clubService = new ClubService(clubRepository, membershipRepository, membershipRequestRepository,
                userRepository, wikiArticleRepository, notificationService);
    }

    private Club aClub(boolean isPublic) {
        User owner = User.builder().id(UUID.randomUUID()).username("owner").displayName("Owner").build();
        return Club.builder().id(UUID.randomUUID()).name("Chess Club").slug("chess")
                .owner(owner).isPublic(isPublic).category("Games").build();
    }

    @Test
    void joinPublicClubAddsMembershipImmediately() {
        Club club = aClub(true);
        UUID userId = UUID.randomUUID();
        when(clubRepository.findById(club.getId())).thenReturn(Optional.of(club));
        when(membershipRepository.existsByClubIdAndUserId(club.getId(), userId)).thenReturn(false);
        when(userRepository.getReferenceById(userId)).thenReturn(User.builder().id(userId).build());

        JoinResultDto result = clubService.join(club.getId(), userId);

        assertThat(result.status()).isEqualTo("JOINED");
        verify(membershipRepository).save(any(Membership.class));
        verifyNoInteractions(notificationService);
    }

    @Test
    void joinPrivateClubCreatesPendingRequest() {
        Club club = aClub(false);
        UUID userId = UUID.randomUUID();
        when(clubRepository.findById(club.getId())).thenReturn(Optional.of(club));
        when(membershipRepository.existsByClubIdAndUserId(club.getId(), userId)).thenReturn(false);
        when(userRepository.getReferenceById(userId)).thenReturn(User.builder().id(userId).build());
        when(membershipRequestRepository.findByClubIdAndUserId(club.getId(), userId)).thenReturn(Optional.empty());

        JoinResultDto result = clubService.join(club.getId(), userId);

        assertThat(result.status()).isEqualTo("REQUESTED");
        verify(membershipRequestRepository).save(any(MembershipRequest.class));
        verify(notificationService).push(eq(club.getOwner().getId()), eq(userId), any(), any(), any(), any());
    }

    @Test
    void joinRejectsWhenAlreadyAMember() {
        Club club = aClub(true);
        UUID userId = UUID.randomUUID();
        when(clubRepository.findById(club.getId())).thenReturn(Optional.of(club));
        when(membershipRepository.existsByClubIdAndUserId(club.getId(), userId)).thenReturn(true);

        assertThatThrownBy(() -> clubService.join(club.getId(), userId)).isInstanceOf(ConflictException.class);
    }

    @Test
    void setMemberRoleRejectsAssigningOwnerRole() {
        UUID clubId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        Membership ownerMembership = Membership.builder().role(MemberRole.OWNER).build();
        when(membershipRepository.findByClubIdAndUserId(clubId, actorId)).thenReturn(Optional.of(ownerMembership));

        assertThatThrownBy(() -> clubService.setMemberRole(clubId, targetId, actorId, MemberRole.OWNER))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void leaveRejectsWhenActorIsOwner() {
        UUID clubId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Membership ownerMembership = Membership.builder().role(MemberRole.OWNER).build();
        when(membershipRepository.findByClubIdAndUserId(clubId, ownerId)).thenReturn(Optional.of(ownerMembership));

        assertThatThrownBy(() -> clubService.leave(clubId, ownerId)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void createRejectsDuplicateSlug() {
        when(clubRepository.existsBySlug("chess")).thenReturn(true);
        CreateClubRequest req =
                new CreateClubRequest("Chess Club", "chess", "desc", "Games", true, null);

        assertThatThrownBy(() -> clubService.create(req, UUID.randomUUID())).isInstanceOf(ConflictException.class);
    }

    @Test
    void getClubThrowsWhenNotFound() {
        UUID randomId = UUID.randomUUID();
        when(clubRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clubService.getClub(randomId.toString(), null))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

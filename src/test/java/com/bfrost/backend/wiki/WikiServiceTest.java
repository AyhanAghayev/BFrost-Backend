package com.bfrost.backend.wiki;

import com.bfrost.backend.club.Club;
import com.bfrost.backend.club.ClubRepository;
import com.bfrost.backend.club.ClubStatus;
import com.bfrost.backend.club.MemberRole;
import com.bfrost.backend.club.Membership;
import com.bfrost.backend.club.MembershipRepository;
import com.bfrost.backend.common.exception.ForbiddenException;
import com.bfrost.backend.common.exception.ResourceNotFoundException;
import com.bfrost.backend.user.User;
import com.bfrost.backend.user.UserRepository;
import com.bfrost.backend.wiki.dto.CreateWikiRequest;
import com.bfrost.backend.wiki.dto.UpdateWikiRequest;
import com.bfrost.backend.wiki.dto.WikiArticleDto;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WikiServiceTest {

    @Mock private WikiArticleRepository wikiRepository;
    @Mock private ClubRepository clubRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private UserRepository userRepository;

    private WikiService wikiService;

    @BeforeEach
    void setUp() {
        wikiService = new WikiService(wikiRepository, clubRepository, membershipRepository, userRepository);
    }

    private Club aClub(ClubStatus status, UUID ownerId) {
        User owner = User.builder().id(ownerId).username("owner").build();
        return Club.builder().id(UUID.randomUUID()).name("Chess Club").slug("chess")
                .owner(owner).status(status).category("Games").build();
    }

    private WikiArticle anArticle(Club club, User author) {
        return WikiArticle.builder().id(UUID.randomUUID()).club(club).author(author)
                .title("Rules").summary("summary").body("body").build();
    }

    @Test
    void getByIdThrowsWhenArticleNotFound() {
        UUID articleId = UUID.randomUUID();
        when(wikiRepository.findById(articleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> wikiService.getById(articleId, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getByIdHidesArticleFromPendingClubForNonOwner() {
        UUID ownerId = UUID.randomUUID();
        Club club = aClub(ClubStatus.PENDING, ownerId);
        WikiArticle article = anArticle(club, User.builder().id(ownerId).build());
        when(wikiRepository.findById(article.getId())).thenReturn(Optional.of(article));

        assertThatThrownBy(() -> wikiService.getById(article.getId(), UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getByIdReturnsArticleFromPendingClubForOwner() {
        UUID ownerId = UUID.randomUUID();
        Club club = aClub(ClubStatus.PENDING, ownerId);
        WikiArticle article = anArticle(club, User.builder().id(ownerId).username("owner").build());
        when(wikiRepository.findById(article.getId())).thenReturn(Optional.of(article));
        when(membershipRepository.findByClubIdAndUserId(club.getId(), ownerId)).thenReturn(Optional.empty());

        WikiArticleDto dto = wikiService.getById(article.getId(), ownerId);

        assertThat(dto.title()).isEqualTo("Rules");
    }

    @Test
    void createRejectsNonModerator() {
        UUID clubId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Membership membership = Membership.builder().role(MemberRole.MEMBER).build();
        when(membershipRepository.findByClubIdAndUserId(clubId, userId)).thenReturn(Optional.of(membership));
        CreateWikiRequest req = new CreateWikiRequest("Rules", "summary", "body", false);

        assertThatThrownBy(() -> wikiService.create(clubId, req, userId)).isInstanceOf(ForbiddenException.class);
        verify(wikiRepository, never()).save(any());
    }

    @Test
    void createClearsPreviousFeaturedArticleWhenNewOneIsFeatured() {
        UUID clubId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Membership membership = Membership.builder().role(MemberRole.MODERATOR).build();
        when(membershipRepository.findByClubIdAndUserId(clubId, userId)).thenReturn(Optional.of(membership));
        when(userRepository.getReferenceById(userId)).thenReturn(User.builder().id(userId).build());
        when(clubRepository.getReferenceById(clubId)).thenReturn(Club.builder().id(clubId).build());
        CreateWikiRequest req = new CreateWikiRequest("Rules", "summary", "body", true);

        wikiService.create(clubId, req, userId);

        verify(wikiRepository).clearFeatured(clubId);
        verify(wikiRepository).save(any(WikiArticle.class));
    }

    @Test
    void updateThrowsWhenArticleNotFound() {
        UUID articleId = UUID.randomUUID();
        when(wikiRepository.findById(articleId)).thenReturn(Optional.empty());
        UpdateWikiRequest req = new UpdateWikiRequest("New title", null, null, null);

        assertThatThrownBy(() -> wikiService.update(articleId, req, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateRejectsNonModerator() {
        UUID ownerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        Club club = aClub(ClubStatus.APPROVED, ownerId);
        WikiArticle article = anArticle(club, User.builder().id(ownerId).build());
        when(wikiRepository.findById(article.getId())).thenReturn(Optional.of(article));
        when(membershipRepository.findByClubIdAndUserId(club.getId(), otherId)).thenReturn(Optional.empty());
        UpdateWikiRequest req = new UpdateWikiRequest("New title", null, null, null);

        assertThatThrownBy(() -> wikiService.update(article.getId(), req, otherId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void deleteRejectsNonModerator() {
        UUID ownerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        Club club = aClub(ClubStatus.APPROVED, ownerId);
        WikiArticle article = anArticle(club, User.builder().id(ownerId).build());
        when(wikiRepository.findById(article.getId())).thenReturn(Optional.of(article));
        when(membershipRepository.findByClubIdAndUserId(club.getId(), otherId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> wikiService.delete(article.getId(), otherId)).isInstanceOf(ForbiddenException.class);
        verify(wikiRepository, never()).delete(any());
    }

    @Test
    void deleteSucceedsForModerator() {
        UUID ownerId = UUID.randomUUID();
        Club club = aClub(ClubStatus.APPROVED, ownerId);
        WikiArticle article = anArticle(club, User.builder().id(ownerId).build());
        when(wikiRepository.findById(article.getId())).thenReturn(Optional.of(article));
        Membership membership = Membership.builder().role(MemberRole.OWNER).build();
        when(membershipRepository.findByClubIdAndUserId(club.getId(), ownerId)).thenReturn(Optional.of(membership));

        wikiService.delete(article.getId(), ownerId);

        verify(wikiRepository).delete(article);
    }

    @Test
    void listByClubThrowsWhenClubNotFoundBySlugOrId() {
        when(clubRepository.findBySlug("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> wikiService.listByClub("unknown", UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

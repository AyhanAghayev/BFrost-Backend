package com.bfrost.backend.wiki;

import com.bfrost.backend.club.Club;
import com.bfrost.backend.club.ClubRepository;
import com.bfrost.backend.club.ClubStatus;
import com.bfrost.backend.club.MemberRole;
import com.bfrost.backend.club.MembershipRepository;
import com.bfrost.backend.common.exception.ForbiddenException;
import com.bfrost.backend.common.exception.ResourceNotFoundException;
import com.bfrost.backend.user.User;
import com.bfrost.backend.user.UserRepository;
import com.bfrost.backend.wiki.dto.CreateWikiRequest;
import com.bfrost.backend.wiki.dto.UpdateWikiRequest;
import com.bfrost.backend.wiki.dto.WikiArticleDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WikiService {

    private final WikiArticleRepository wikiRepository;
    private final ClubRepository clubRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<WikiArticleDto> listByClub(String clubIdentifier, UUID currentUserId) {
        Club club = resolveReadableClub(clubIdentifier, currentUserId);
        return wikiRepository.findByClubIdOrderByFeaturedDescUpdatedAtDesc(club.getId()).stream()
                .map(w -> WikiArticleDto.from(w, canManage(club.getId(), currentUserId)))
                .toList();
    }

    @Transactional(readOnly = true)
    public WikiArticleDto getById(UUID articleId, UUID currentUserId) {
        WikiArticle article = wikiRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found"));
        if (article.getClub().getStatus() == ClubStatus.PENDING
                && (currentUserId == null || !article.getClub().getOwner().getId().equals(currentUserId))) {
            throw new ResourceNotFoundException("Article not found");
        }
        return WikiArticleDto.from(article, canManage(article.getClub().getId(), currentUserId));
    }

    @Transactional
    public WikiArticleDto create(UUID clubId, CreateWikiRequest req, UUID userId) {
        requireModerator(clubId, userId);
        if (req.featured()) wikiRepository.clearFeatured(clubId);
        User author = userRepository.getReferenceById(userId);
        WikiArticle article = WikiArticle.builder()
                .club(clubRepository.getReferenceById(clubId))
                .author(author)
                .lastEditor(author)
                .title(req.title())
                .summary(req.summary())
                .body(req.body())
                .featured(req.featured())
                .build();
        wikiRepository.save(article);
        return WikiArticleDto.from(article, true);
    }

    @Transactional
    public WikiArticleDto update(UUID articleId, UpdateWikiRequest req, UUID userId) {
        WikiArticle article = wikiRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found"));
        requireModerator(article.getClub().getId(), userId);
        if (req.title() != null)   article.setTitle(req.title());
        if (req.summary() != null) article.setSummary(req.summary());
        if (req.body() != null)    article.setBody(req.body());
        if (req.featured() != null) {
            if (req.featured() && !article.isFeatured()) {
                wikiRepository.clearFeatured(article.getClub().getId());
            }
            article.setFeatured(req.featured());
        }
        article.setLastEditor(userRepository.getReferenceById(userId));
        wikiRepository.save(article);
        return WikiArticleDto.from(article, true);
    }

    @Transactional
    public void delete(UUID articleId, UUID userId) {
        WikiArticle article = wikiRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found"));
        requireModerator(article.getClub().getId(), userId);
        wikiRepository.delete(article);
    }

    @Transactional(readOnly = true)
    public List<WikiArticleDto> getByAuthor(UUID authorId, UUID currentUserId) {
        boolean self = authorId.equals(currentUserId);
        return wikiRepository.findByAuthorIdOrderByUpdatedAtDesc(authorId).stream()
                .filter(w -> self || w.getClub().getStatus() == ClubStatus.APPROVED)
                .map(w -> WikiArticleDto.from(w, canManage(w.getClub().getId(), currentUserId)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WikiArticleDto> getFeed(UUID userId) {
        return wikiRepository.findFeedForUser(userId).stream()
                .map(w -> WikiArticleDto.from(w, canManage(w.getClub().getId(), userId)))
                .toList();
    }

    private Club resolveReadableClub(String identifier, UUID currentUserId) {
        Club club = resolveClub(identifier);
        if (club.getStatus() == ClubStatus.PENDING
                && (currentUserId == null || !club.getOwner().getId().equals(currentUserId))) {
            throw new ResourceNotFoundException("Club not found: " + identifier);
        }
        return club;
    }

    private Club resolveClub(String identifier) {
        try {
            return clubRepository.findById(UUID.fromString(identifier))
                    .orElseThrow(() -> new ResourceNotFoundException("Club not found: " + identifier));
        } catch (IllegalArgumentException notAUuid) {
            return clubRepository.findBySlug(identifier)
                    .orElseThrow(() -> new ResourceNotFoundException("Club not found: " + identifier));
        }
    }

    private boolean canManage(UUID clubId, UUID userId) {
        if (userId == null) return false;
        return membershipRepository.findByClubIdAndUserId(clubId, userId)
                .map(m -> m.getRole() != MemberRole.MEMBER)
                .orElse(false);
    }

    private void requireModerator(UUID clubId, UUID userId) {
        if (!canManage(clubId, userId)) {
            throw new ForbiddenException("Moderator or owner required to manage the wiki");
        }
    }
}

package com.bfrost.backend.wiki;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface WikiArticleRepository extends JpaRepository<WikiArticle, UUID> {

    List<WikiArticle> findByClubIdOrderByFeaturedDescUpdatedAtDesc(UUID clubId);

    long countByClubId(UUID clubId);

    List<WikiArticle> findByAuthorIdOrderByUpdatedAtDesc(UUID authorId);

    // aarticles from the clubs a user belongs to — the personal "Wiki Hub" feed.
    @Query("""
        SELECT w FROM WikiArticle w
        WHERE w.club.id IN (SELECT m.club.id FROM Membership m WHERE m.user.id = :userId)
        ORDER BY w.featured DESC, w.updatedAt DESC
        """)
    List<WikiArticle> findFeedForUser(UUID userId);

    // clear the current featured article for a club. Only one may be present
    @Modifying
    @Query("UPDATE WikiArticle w SET w.featured = false WHERE w.club.id = :clubId AND w.featured = true")
    void clearFeatured(UUID clubId);
}

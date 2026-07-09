package com.bfrost.backend.club;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClubRepository extends JpaRepository<Club, UUID> {
    Optional<Club> findBySlug(String slug);
    boolean existsBySlug(String slug);

    @Query(value = "SELECT * FROM clubs WHERE status = 'APPROVED' AND search_vector @@ plainto_tsquery('english', :query) ORDER BY ts_rank(search_vector, plainto_tsquery('english', :query)) DESC LIMIT :limit", nativeQuery = true)
    List<Club> searchByText(String query, int limit);

    List<Club> findByCategory(String category);

    @Query(value = """
        SELECT c.* FROM clubs c
        LEFT JOIN (SELECT club_id, COUNT(*) AS cnt FROM memberships GROUP BY club_id) m
            ON c.id = m.club_id
        WHERE c.status = 'APPROVED'
        ORDER BY COALESCE(m.cnt, 0) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Club> findAllOrderByMemberCount(int limit);

    List<Club> findByStatusOrderByCreatedAtDesc(ClubStatus status);
    long countByStatus(ClubStatus status);

    interface TagCount {
        String getTag();
        long getCnt();
        String getCategory();
    }

    // Most-used club tags, with a representative category — drives "trending topics".
    @Query(value = """
        SELECT ct.tag AS tag, COUNT(*) AS cnt, MIN(c.category) AS category
        FROM club_tags ct
        JOIN clubs c ON c.id = ct.club_id
        GROUP BY ct.tag
        ORDER BY cnt DESC, ct.tag ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<TagCount> findTrendingTags(int limit);
}

package com.bfrost.backend.post;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {

    List<Post> findByTargetTypeAndTargetIdOrderByCreatedAtDescIdDesc(TargetType targetType, UUID targetId);

    @Query(value = """
        SELECT * FROM posts
        WHERE target_type = :#{#targetType.name()} AND target_id = :targetId
          AND (created_at < :cursor OR (created_at = :cursor AND id < :cursorId))
        ORDER BY created_at DESC, id DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Post> findByTargetWithCursor(TargetType targetType, UUID targetId, Instant cursor, UUID cursorId, int limit);

    @Query(value = """
        (SELECT p.* FROM posts p
         JOIN memberships m ON m.club_id = p.target_id AND p.target_type = 'CLUB_PAGE'
         WHERE m.user_id = :userId
           AND (CAST(:cursor AS TIMESTAMPTZ) IS NULL OR p.created_at < :cursor
                OR (p.created_at = :cursor AND p.id < CAST(:cursorId AS UUID))))
        UNION ALL
        (SELECT p.* FROM posts p
         JOIN follows f ON f.followee_id = p.target_id AND p.target_type = 'USER_PAGE'
         WHERE f.follower_id = :userId
           AND (CAST(:cursor AS TIMESTAMPTZ) IS NULL OR p.created_at < :cursor
                OR (p.created_at = :cursor AND p.id < CAST(:cursorId AS UUID))))
        ORDER BY created_at DESC, id DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Post> findFeedForUser(UUID userId, Instant cursor, UUID cursorId, int limit);

    @Query(value = "SELECT * FROM posts WHERE search_vector @@ plainto_tsquery('english', :query) ORDER BY ts_rank(search_vector, plainto_tsquery('english', :query)) DESC LIMIT :limit", nativeQuery = true)
    List<Post> searchByText(String query, int limit);
}

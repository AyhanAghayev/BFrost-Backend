package com.bfrost.backend.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FollowRepository extends JpaRepository<Follow, UUID> {
    Optional<Follow> findByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);
    boolean existsByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);
    long countByFollowerId(UUID followerId);
    long countByFolloweeId(UUID followeeId);

    List<Follow> findByFollowerIdOrderByCreatedAtDesc(UUID followerId);
    List<Follow> findByFolloweeIdOrderByCreatedAtDesc(UUID followeeId);

    @Query("""
        SELECT f FROM Follow f WHERE f.follower.id = :userA AND f.followee.id = :userB
        AND EXISTS (SELECT 1 FROM Follow f2 WHERE f2.follower.id = :userB AND f2.followee.id = :userA)
    """)
    Optional<Follow> findMutualFollow(UUID userA, UUID userB);

    @Query("""
        SELECT f.followee.id FROM Follow f
        WHERE f.follower.id = :userId
        AND EXISTS (SELECT 1 FROM Follow f2 WHERE f2.follower.id = f.followee.id AND f2.followee.id = :userId)
    """)
    List<UUID> findFriendIds(UUID userId);
}
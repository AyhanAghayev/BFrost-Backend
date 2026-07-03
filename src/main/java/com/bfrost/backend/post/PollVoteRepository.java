package com.bfrost.backend.post;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface PollVoteRepository extends JpaRepository<PollVote, UUID> {
    boolean existsByOptionIdAndUserId(UUID optionId, UUID userId);

    @Query("SELECT COUNT(pv) FROM PollVote pv WHERE pv.option.post.id = :postId AND pv.user.id = :userId")
    long countVotesByUserOnPost(UUID postId, UUID userId);
}

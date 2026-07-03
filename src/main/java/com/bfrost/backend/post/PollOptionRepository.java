package com.bfrost.backend.post;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface PollOptionRepository extends JpaRepository<PollOption, UUID> {
    List<PollOption> findByPostIdOrderByPosition(UUID postId);

    @Query("SELECT pv.option.id FROM PollVote pv WHERE pv.user.id = :userId AND pv.option.post.id = :postId")
    List<UUID> findVotedOptionIdsByUserAndPost(UUID userId, UUID postId);
}

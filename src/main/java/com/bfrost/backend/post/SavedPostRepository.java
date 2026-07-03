package com.bfrost.backend.post;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SavedPostRepository extends JpaRepository<SavedPost, UUID> {
    boolean existsByPostIdAndUserId(UUID postId, UUID userId);
    void deleteByPostIdAndUserId(UUID postId, UUID userId);
    List<SavedPost> findByUserIdOrderByCreatedAtDesc(UUID userId);
}

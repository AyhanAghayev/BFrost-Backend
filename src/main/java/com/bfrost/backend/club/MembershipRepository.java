package com.bfrost.backend.club;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {
    Optional<Membership> findByClubIdAndUserId(UUID clubId, UUID userId);
    boolean existsByClubIdAndUserId(UUID clubId, UUID userId);
    long countByClubId(UUID clubId);
    long countByUserId(UUID userId);
    List<Membership> findByClubId(UUID clubId);
    List<Membership> findByUserId(UUID userId);

    @Query("SELECT m FROM Membership m WHERE m.club.id = :clubId AND m.role IN ('OWNER', 'MODERATOR')")
    List<Membership> findModerators(UUID clubId);
}
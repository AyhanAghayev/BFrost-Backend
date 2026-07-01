package com.bfrost.backend.club;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRequestRepository extends JpaRepository<MembershipRequest, UUID> {
    Optional<MembershipRequest> findByClubIdAndUserId(UUID clubId, UUID userId);
    List<MembershipRequest> findByClubIdAndStatus(UUID clubId, RequestStatus status);
    boolean existsByClubIdAndUserIdAndStatus(UUID clubId, UUID userId, RequestStatus status);
}

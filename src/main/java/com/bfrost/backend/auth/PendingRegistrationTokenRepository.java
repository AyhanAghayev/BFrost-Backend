package com.bfrost.backend.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface PendingRegistrationTokenRepository extends JpaRepository<PendingRegistrationToken, UUID> {
    Optional<PendingRegistrationToken> findByToken(String token);

    @Modifying
    @Query("DELETE FROM PendingRegistrationToken prt where prt.user.id = :userId")
    void deleteAllByUserId(UUID userId);
}

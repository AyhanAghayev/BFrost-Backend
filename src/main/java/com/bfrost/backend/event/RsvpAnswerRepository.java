package com.bfrost.backend.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RsvpAnswerRepository extends JpaRepository<RsvpAnswer, UUID> {
    List<RsvpAnswer> findByRsvpId(UUID rsvpId);
    List<RsvpAnswer> findByRsvpIdIn(List<UUID> rsvpIds);
    void deleteByRsvpId(UUID rsvpId);
}

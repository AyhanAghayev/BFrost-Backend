package com.bfrost.backend.club.dto;

import com.bfrost.backend.club.MembershipRequest;

import java.time.Instant;
import java.util.UUID;

public record JoinRequestDto(
        UUID    requestId,
        UUID userId,
        String  username,
        String  displayName,
        String  profilePictureUrl,
        Instant createdAt
) {
    public static JoinRequestDto from(MembershipRequest r) {
        return new JoinRequestDto(
                r.getId(),
                r.getUser().getId(),
                r.getUser().getUsername(),
                r.getUser().getDisplayName(),
                r.getUser().getProfilePictureUrl(),
                r.getCreatedAt()
        );
    }
}

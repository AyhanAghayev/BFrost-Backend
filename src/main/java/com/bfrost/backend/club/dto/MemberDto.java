package com.bfrost.backend.club.dto;

import com.bfrost.backend.club.Membership;

import java.time.Instant;
import java.util.UUID;

public record MemberDto(
        UUID userId,
        String  username,
        String  displayName,
        String  profilePictureUrl,
        String  role,
        Instant joinedAt
) {
    public static MemberDto from(Membership m) {
        return new MemberDto(
                m.getUser().getId(),
                m.getUser().getUsername(),
                m.getUser().getDisplayName(),
                m.getUser().getProfilePictureUrl(),
                m.getRole().name(),
                m.getJoinedAt()
        );
    }
}
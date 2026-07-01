package com.bfrost.backend.club.dto;

import com.bfrost.backend.club.Club;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record ClubDto(
        UUID    id,
        String  name,
        String  slug,
        String  description,
        UUID    ownerId,
        boolean isPublic,
        String  category,
        String  coverImageUrl,
        String  logoUrl,
        Set<String> tags,
        long    memberCount,
        boolean isMember,
        String  memberRole,
        boolean hasPendingRequest,
        Instant createdAt
) {
    public static ClubDto from(Club c, long memberCount, boolean isMember, String memberRole, boolean hasPendingRequest) {
        return new ClubDto(
                c.getId(), c.getName(), c.getSlug(), c.getDescription(),
                c.getOwner().getId(), c.isPublic(), c.getCategory(),
                c.getCoverImageUrl(), c.getLogoUrl(), c.getTags(),
                memberCount, isMember, memberRole, hasPendingRequest, c.getCreatedAt()
        );
    }
}

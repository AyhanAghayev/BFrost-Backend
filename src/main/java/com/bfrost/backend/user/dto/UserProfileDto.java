package com.bfrost.backend.user.dto;

import com.bfrost.backend.user.User;

import java.time.Instant;
import java.util.UUID;

public record UserProfileDto(
    UUID    id,
    String  username,
    String  email,
    String  displayName,
    String  bio,
    String  profilePictureUrl,
    String  backgroundUrl,
    String  university,
    String  department,
    boolean verified,
    String  role,
    long    followerCount,
    long    followingCount,
    boolean isFollowedByCurrentUser,
    Instant createdAt
) {
    public static UserProfileDto from(User u, long followers, long following, boolean isFollowed, boolean includeEmail) {
        return new UserProfileDto(
            u.getId(), u.getUsername(), includeEmail ? u.getEmail() : null, u.getDisplayName(), u.getBio(),
            u.getProfilePictureUrl(), u.getBackgroundUrl(), u.getUniversity(),
            u.getDepartment(), u.isVerified(), u.getRole().name(), followers, following, isFollowed,
            u.getCreatedAt()
        );
    }
}

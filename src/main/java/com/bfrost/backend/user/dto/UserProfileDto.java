package com.bfrost.user.dto;

import com.bfrost.user.User;

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
    long    followerCount,
    long    followingCount,
    boolean isFollowedByCurrentUser,
    Instant createdAt
) {
    public static UserProfileDto from(User u, long followers, long following, boolean isFollowed, boolean includeEmail) {
        return new UserProfileDto(
            u.getId(), u.getUsername(), includeEmail ? u.getEmail() : null, u.getDisplayName(), u.getBio(),
            u.getProfilePictureUrl(), u.getBackgroundUrl(), u.getUniversity(),
            u.getDepartment(), u.isVerified(), followers, following, isFollowed,
            u.getCreatedAt()
        );
    }
}

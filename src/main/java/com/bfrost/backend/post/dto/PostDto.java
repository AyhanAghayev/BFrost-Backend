package com.bfrost.backend.post.dto;

import com.bfrost.backend.post.Post;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PostDto(
    UUID    id,
    UUID    authorId,
    String  authorUsername,
    String  authorDisplayName,
    String  authorPictureUrl,
    String  targetType,
    UUID    targetId,
    String  targetName,
    String  targetSlug,
    String  postType,
    String  title,
    String  body,
    String  mediaUrl,
    String  linkUrl,
    String  channel,
    int     likeCount,
    int     dislikeCount,
    int     commentCount,
    String  currentUserReaction,
    boolean saved,
    List<PollOptionDto> pollOptions,
    Instant createdAt
) {
    public static PostDto from(Post p, String reaction, boolean saved, List<PollOptionDto> options,
                               String targetName, String targetSlug) {
        return new PostDto(
            p.getId(),
            p.getAuthor().getId(),
            p.getAuthor().getUsername(),
            p.getAuthor().getDisplayName(),
            p.getAuthor().getProfilePictureUrl(),
            p.getTargetType().name(),
            p.getTargetId(),
            targetName,
            targetSlug,
            p.getPostType().name(),
            p.getTitle(),
            p.getBody(),
            p.getMediaUrl(),
            p.getLinkUrl(),
            p.getChannel(),
            p.getLikeCount(),
            p.getDislikeCount(),
            p.getCommentCount(),
            reaction,
            saved,
            options,
            p.getCreatedAt()
        );
    }
}

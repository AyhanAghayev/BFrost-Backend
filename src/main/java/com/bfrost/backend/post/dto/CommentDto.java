package com.bfrost.backend.post.dto;

import com.bfrost.backend.post.Comment;

import java.time.Instant;
import java.util.UUID;

public record CommentDto(
        UUID    id,
        UUID authorId,
        String  authorUsername,
        String  authorDisplayName,
        String  authorPictureUrl,
        String  body,
        Instant createdAt
) {
    public static CommentDto from(Comment c) {
        return new CommentDto(
                c.getId(),
                c.getAuthor().getId(),
                c.getAuthor().getUsername(),
                c.getAuthor().getDisplayName(),
                c.getAuthor().getProfilePictureUrl(),
                c.getBody(),
                c.getCreatedAt()
        );
    }
}

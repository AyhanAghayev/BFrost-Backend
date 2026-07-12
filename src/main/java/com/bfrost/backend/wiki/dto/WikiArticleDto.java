package com.bfrost.backend.wiki.dto;

import com.bfrost.backend.wiki.WikiArticle;

import java.time.Instant;
import java.util.UUID;

public record WikiArticleDto(
        UUID    id,
        UUID    clubId,
        String  clubSlug,
        String  clubName,
        String  title,
        String  summary,
        String  body,
        boolean featured,
        UUID    authorId,
        String  authorUsername,
        String  authorDisplayName,
        String  authorPictureUrl,
        boolean canManage,
        Instant updatedAt,
        Instant createdAt
) {
    public static WikiArticleDto from(WikiArticle w, boolean canManage) {
        return new WikiArticleDto(
                w.getId(),
                w.getClub().getId(),
                w.getClub().getSlug(),
                w.getClub().getName(),
                w.getTitle(),
                w.getSummary(),
                w.getBody(),
                w.isFeatured(),
                w.getAuthor().getId(),
                w.getAuthor().getUsername(),
                w.getAuthor().getDisplayName(),
                w.getAuthor().getProfilePictureUrl(),
                canManage,
                w.getUpdatedAt(),
                w.getCreatedAt()
        );
    }
}

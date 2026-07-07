package com.bfrost.backend.user.dto;

public record UpdateNotificationPrefsRequest(
        Boolean follow,
        Boolean like,
        Boolean comment,
        Boolean joinRequest
) {
}

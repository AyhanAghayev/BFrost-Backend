package com.bfrost.backend.user.dto;

import com.bfrost.backend.user.User;

public record NotificationPrefsDto(
        boolean follow,
        boolean like,
        boolean comment,
        boolean joinRequest
) {
    public static NotificationPrefsDto from(User u) {
        return new NotificationPrefsDto(
                u.isNotifyFollow(),
                u.isNotifyLike(),
                u.isNotifyComment(),
                u.isNotifyJoinRequest()
        );
    }
}

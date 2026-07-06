package com.bfrost.backend.notification;

import com.bfrost.backend.auth.BFrostUserDetails;
import com.bfrost.backend.notification.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List ;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationDto> list(@AuthenticationPrincipal BFrostUserDetails principal) {
        return notificationService.getNotifications(principal.userId());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal BFrostUserDetails principal) {
        return Map.of("count", notificationService.getUnreadCount(principal.userId()));
    }

    @PostMapping("/mark-all-read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(@AuthenticationPrincipal BFrostUserDetails principal) {
        notificationService.markAllRead(principal.userId());
    }
}

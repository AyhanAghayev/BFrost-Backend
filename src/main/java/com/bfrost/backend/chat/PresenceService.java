package com.bfrost.backend.chat;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PresenceService {

    private final Map<String, String> sessionToUser = new ConcurrentHashMap<>();

    @EventListener
    public void onConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        String sessionId = accessor.getSessionId();
        if (user != null && sessionId != null) {
            sessionToUser.put(sessionId, user.getName());
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        sessionToUser.remove(event.getSessionId());
    }

    public Set<String> onlineUserIds() {
        return new HashSet<>(sessionToUser.values());
    }

    public boolean isOnline(UUID userId) {
        return sessionToUser.containsValue(userId.toString());
    }
}

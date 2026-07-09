package com.bfrost.backend.config;

import com.bfrost.backend.auth.JwtService;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue", "/topic");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // SockJS fallback endpoint…
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:3000", "https://*.bfrost.com")
                .setHandshakeHandler(cookieHandshakeHandler())
                .withSockJS();
        registry.addEndpoint("/ws-native")
                .setAllowedOriginPatterns("http://localhost:3000", "https://*.bfrost.com")
                .setHandshakeHandler(cookieHandshakeHandler());
    }

    private DefaultHandshakeHandler cookieHandshakeHandler() {
        return new DefaultHandshakeHandler() {
            @Override
            protected Principal determineUser(ServerHttpRequest request,
                                              WebSocketHandler wsHandler,
                                              Map<String, Object> attributes) {
                if (request instanceof ServletServerHttpRequest servletRequest) {
                    Cookie[] cookies = servletRequest.getServletRequest().getCookies();
                    if (cookies != null) {
                        for (Cookie cookie : cookies) {
                            if ("accessToken".equals(cookie.getName()) && jwtService.isValid(cookie.getValue())) {
                                UUID userId = jwtService.extractUserId(cookie.getValue());
                                UserDetails details = userDetailsService.loadUserByUsername(userId.toString());
                                return new UsernamePasswordAuthenticationToken(
                                        details, null, details.getAuthorities());
                            }
                        }
                    }
                }
                return null;
            }
        };
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        if (jwtService.isValid(token)) {
                            UUID userId = jwtService.extractUserId(token);
                            UserDetails details = userDetailsService.loadUserByUsername(userId.toString());
                            var auth = new UsernamePasswordAuthenticationToken(
                                    details, null, details.getAuthorities());
                            accessor.setUser(auth);
                        }
                    }
                }
                return message;
            }
        });
    }
}

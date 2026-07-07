package com.bfrost.backend.config;

import com.bfrost.backend.auth.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
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
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final String ACCESS_TOKEN_ATTR = "accessToken";

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
                .addInterceptors(new CookieHandshakeInterceptor())
                .withSockJS();
        // …and a raw WebSocket endpoint the @stomp/stompjs client connects to directly.
        registry.addEndpoint("/ws-native")
                .setAllowedOriginPatterns("http://localhost:3000", "https://*.bfrost.com")
                .addInterceptors(new CookieHandshakeInterceptor());
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                    Object token = sessionAttributes != null ? sessionAttributes.get(ACCESS_TOKEN_ATTR) : null;
                    if (token instanceof String accessToken && jwtService.isValid(accessToken)) {
                        UUID userId = jwtService.extractUserId(accessToken);
                        UserDetails details = userDetailsService.loadUserByUsername(userId.toString());
                        var auth = new UsernamePasswordAuthenticationToken(
                                details, null, details.getAuthorities());
                        accessor.setUser(auth);
                    }
                }
                return message;
            }
        });
    }

    private static class CookieHandshakeInterceptor implements HandshakeInterceptor {
        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                        WebSocketHandler wsHandler, Map<String, Object> attributes) {
            if (request instanceof ServletServerHttpRequest servletRequest) {
                HttpServletRequest httpRequest = servletRequest.getServletRequest();
                Cookie[] cookies = httpRequest.getCookies();
                if (cookies != null) {
                    Arrays.stream(cookies)
                            .filter(c -> ACCESS_TOKEN_ATTR.equals(c.getName()))
                            .findFirst()
                            .ifPresent(c -> attributes.put(ACCESS_TOKEN_ATTR, c.getValue()));
                }
            }
            return true;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Exception exception) {
        }
    }
}

package com.bfrost.backend.auth.oauth;

import com.bfrost.backend.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public record BFrostOAuth2User(User user, Map<String, Object> attributes) implements OAuth2User, BFrostPrincipal {

    @Override public Map<String, Object> getAttributes() { return attributes; }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override public String getName() { return user.getId().toString(); }
}

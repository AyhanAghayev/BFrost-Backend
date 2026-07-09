package com.bfrost.backend.auth;

import com.bfrost.backend.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public record BFrostUserDetails(User user) implements UserDetails {

    public UUID userId(){ return user.getId(); }

    @Override public String getUsername(){return user.getId().toString();}
    @Override public String getPassword(){return user.getPasswordHash();}
    @Override public Collection<? extends GrantedAuthority> getAuthorities(){
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }
    @Override public boolean isAccountNonExpired() {return true;}
    @Override public boolean isAccountNonLocked(){return true;}
    @Override public boolean isCredentialsNonExpired(){return true;}
    @Override public boolean isEnabled(){return true;}
}

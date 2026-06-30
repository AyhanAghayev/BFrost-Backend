package com.bfrost.backend.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenExpiryMs;

    public JwtService(
            @Value("${bfrost.jwt.secret}") String secret,
            @Value("$bfrost.jwt.access-token-expiry-ms") long accessTokenExpiryMs
    ){
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiryMs = accessTokenExpiryMs;
    }

    public String generateAccessToken(UUID userId, String username){
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .issuedAt(new Date(now))
                .expiration(new Date(now+accessTokenExpiryMs))
                .signWith(signingKey)
                .compact();
    }

    public Claims validateAndParse(String token){
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractUserId(String token){
        return UUID.fromString(validateAndParse(token).getSubject());
    }

    public boolean isValid(String token){
        try{
            validateAndParse(token);
            return true;
        }
        catch (Exception e){
            return false;
        }
    }
}
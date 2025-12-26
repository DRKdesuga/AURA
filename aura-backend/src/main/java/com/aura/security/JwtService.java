package com.aura.security;

import com.aura.domain.UserEntity;
import com.aura.domain.UserRole;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtService {

    private final SecretKey key;
    private final long expirationSeconds;

    public JwtService(
            @Value("${security.jwt.secret:dev-secret-key-change-me-please-32-bytes}") String secret,
            @Value("${security.jwt.expiration-seconds:3600}") long expirationSeconds
    ) {
        byte[] keyBytes = decodeKey(secret);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(UserEntity user) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expirationSeconds);
        return Jwts.builder()
                .setSubject(user.getId().toString())
                .addClaims(Map.of(
                        "role", user.getRole().name(),
                        "email", user.getEmail()
                ))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public AuthenticatedUser parse(String token) {
        Jws<Claims> claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
        Claims body = claims.getBody();
        UUID userId = UUID.fromString(body.getSubject());
        UserRole role = UserRole.valueOf((String) body.get("role"));
        String email = (String) body.get("email");
        return new AuthenticatedUser(userId, email, role);
    }

    private byte[] decodeKey(String secret) {
        try {
            return Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException e) {
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }
}

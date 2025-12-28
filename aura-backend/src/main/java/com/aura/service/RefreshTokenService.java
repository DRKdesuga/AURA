package com.aura.service;

import com.aura.domain.RefreshTokenEntity;
import com.aura.domain.UserEntity;
import com.aura.error.AuraErrorCode;
import com.aura.error.AuraException;
import com.aura.repository.RefreshTokenRepository;
import com.aura.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 64;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final long refreshExpirationSeconds;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService,
            @Value("${security.jwt.refresh-expiration-seconds:604800}") long refreshExpirationSeconds
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.refreshExpirationSeconds = refreshExpirationSeconds;
    }

    @Transactional
    public String issue(UserEntity user, String userAgent, String ipAddress) {
        String rawToken = generateTokenValue();
        Instant now = Instant.now();
        RefreshTokenEntity entity = RefreshTokenEntity.builder()
                .tokenHash(hashToken(rawToken))
                .user(user)
                .createdAt(now)
                .expiresAt(now.plusSeconds(refreshExpirationSeconds))
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .build();
        refreshTokenRepository.save(entity);
        return rawToken;
    }

    @Transactional
    public RefreshResult refresh(String rawToken, String userAgent, String ipAddress) {
        RefreshTokenEntity existing = requireValid(rawToken);
        UserEntity user = existing.getUser();
        if (!user.isEnabled()) {
            throw new AuraException(AuraErrorCode.FORBIDDEN, "User is disabled");
        }
        Instant now = Instant.now();
        existing.setRevokedAt(now);
        refreshTokenRepository.save(existing);

        String newRefreshToken = issue(user, userAgent, ipAddress);
        String accessToken = jwtService.generateToken(user);
        return new RefreshResult(accessToken, newRefreshToken, user);
    }

    @Transactional
    public void revoke(String rawToken) {
        RefreshTokenEntity token = requireToken(rawToken);
        if (token.getRevokedAt() != null) {
            return;
        }
        token.setRevokedAt(Instant.now());
        refreshTokenRepository.save(token);
    }

    @Transactional
    public int revokeAll(UUID userId) {
        List<RefreshTokenEntity> active = refreshTokenRepository.findAllByUser_IdAndRevokedAtIsNull(userId);
        if (active.isEmpty()) {
            return 0;
        }
        Instant now = Instant.now();
        active.forEach(token -> token.setRevokedAt(now));
        refreshTokenRepository.saveAll(active);
        return active.size();
    }

    private RefreshTokenEntity requireValid(String rawToken) {
        RefreshTokenEntity token = requireToken(rawToken);
        if (token.getRevokedAt() != null) {
            throw new AuraException(AuraErrorCode.AUTH_REFRESH_REVOKED, "Refresh token revoked");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new AuraException(AuraErrorCode.AUTH_REFRESH_EXPIRED, "Refresh token expired");
        }
        return token;
    }

    private RefreshTokenEntity requireToken(String rawToken) {
        String token = rawToken == null ? null : rawToken.trim();
        if (token == null || token.isEmpty()) {
            throw new AuraException(AuraErrorCode.AUTH_REFRESH_INVALID, "Refresh token required");
        }
        String hash = hashToken(token);
        return refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new AuraException(AuraErrorCode.AUTH_REFRESH_INVALID, "Refresh token not found"));
    }

    private String generateTokenValue() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public record RefreshResult(String accessToken, String refreshToken, UserEntity user) {
        public RefreshResult {
            Objects.requireNonNull(accessToken, "accessToken");
            Objects.requireNonNull(refreshToken, "refreshToken");
            Objects.requireNonNull(user, "user");
        }
    }
}

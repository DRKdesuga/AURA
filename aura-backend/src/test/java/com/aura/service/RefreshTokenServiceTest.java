package com.aura.service;

import com.aura.domain.RefreshTokenEntity;
import com.aura.domain.UserEntity;
import com.aura.domain.UserRole;
import com.aura.error.AuraErrorCode;
import com.aura.error.AuraException;
import com.aura.repository.RefreshTokenRepository;
import com.aura.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock JwtService jwtService;

    RefreshTokenService refreshTokenService;
    UserEntity user;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, jwtService, 3600L);
        user = UserEntity.builder()
                .id(UUID.randomUUID())
                .email("user@aura.local")
                .role(UserRole.USER)
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("refresh: rotates token and returns new access token")
    void refresh_rotatesToken_andReturnsNewAccessToken() {
        String rawToken = "refresh-token";
        String hash = hash(rawToken);
        RefreshTokenEntity existing = RefreshTokenEntity.builder()
                .id(UUID.randomUUID())
                .tokenHash(hash)
                .user(user)
                .createdAt(Instant.now().minusSeconds(60))
                .expiresAt(Instant.now().plusSeconds(600))
                .build();

        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(existing));
        when(refreshTokenRepository.save(any(RefreshTokenEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(user)).thenReturn("access-token");

        RefreshTokenService.RefreshResult result = refreshTokenService.refresh(rawToken, "agent", "127.0.0.1");

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.user()).isEqualTo(user);
        assertThat(existing.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository, times(2)).save(any(RefreshTokenEntity.class));
    }

    @Test
    @DisplayName("refresh: throws when token is revoked")
    void refresh_throws_whenRevoked() {
        String rawToken = "revoked-token";
        String hash = hash(rawToken);
        RefreshTokenEntity revoked = RefreshTokenEntity.builder()
                .id(UUID.randomUUID())
                .tokenHash(hash)
                .user(user)
                .expiresAt(Instant.now().plusSeconds(600))
                .revokedAt(Instant.now())
                .build();

        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> refreshTokenService.refresh(rawToken, null, null))
                .isInstanceOf(AuraException.class)
                .extracting("code").isEqualTo(AuraErrorCode.AUTH_REFRESH_REVOKED);

        verify(refreshTokenRepository, never()).save(any(RefreshTokenEntity.class));
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("refresh: throws when token is expired")
    void refresh_throws_whenExpired() {
        String rawToken = "expired-token";
        String hash = hash(rawToken);
        RefreshTokenEntity expired = RefreshTokenEntity.builder()
                .id(UUID.randomUUID())
                .tokenHash(hash)
                .user(user)
                .expiresAt(Instant.now().minusSeconds(5))
                .build();

        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> refreshTokenService.refresh(rawToken, null, null))
                .isInstanceOf(AuraException.class)
                .extracting("code").isEqualTo(AuraErrorCode.AUTH_REFRESH_EXPIRED);

        verify(refreshTokenRepository, never()).save(any(RefreshTokenEntity.class));
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("logout: revokes token")
    void logout_revokesToken() {
        String rawToken = "logout-token";
        String hash = hash(rawToken);
        RefreshTokenEntity existing = RefreshTokenEntity.builder()
                .id(UUID.randomUUID())
                .tokenHash(hash)
                .user(user)
                .expiresAt(Instant.now().plusSeconds(600))
                .build();

        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(existing));
        when(refreshTokenRepository.save(any(RefreshTokenEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        refreshTokenService.revoke(rawToken);

        assertThat(existing.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(existing);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

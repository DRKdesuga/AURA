package com.aura.controller;

import com.aura.dto.*;
import com.aura.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin
public class AuthController {

    private final AuthService authService;
    @Value("${security.jwt.refresh-expiration-seconds:604800}")
    private long refreshExpirationSeconds;
    @Value("${security.jwt.refresh-cookie-secure:false}")
    private boolean refreshCookieSecure;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO body, HttpServletRequest request) {
        AuthService.AuthTokens tokens = authService.register(body, request.getHeader("User-Agent"), resolveIp(request));
        ResponseCookie cookie = buildRefreshCookie(tokens.refreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(tokens.response());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO body, HttpServletRequest request) {
        AuthService.AuthTokens tokens = authService.login(body, request.getHeader("User-Agent"), resolveIp(request));
        ResponseCookie cookie = buildRefreshCookie(tokens.refreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(tokens.response());
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> me() {
        return ResponseEntity.ok(authService.me());
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshCookie,
            @RequestBody(required = false) RefreshTokenRequestDTO body,
            HttpServletRequest request
    ) {
        String refreshToken = resolveRefreshToken(refreshCookie, body);
        AuthService.AuthTokens tokens = authService.refresh(refreshToken, request.getHeader("User-Agent"), resolveIp(request));
        ResponseCookie cookie = buildRefreshCookie(tokens.refreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(tokens.response());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshCookie,
            @RequestBody(required = false) RefreshTokenRequestDTO body
    ) {
        String refreshToken = resolveRefreshToken(refreshCookie, body);
        authService.logout(refreshToken);
        ResponseCookie cookie = clearRefreshCookie();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<LogoutAllResponseDTO> logoutAll() {
        int revoked = authService.logoutAll();
        return ResponseEntity.ok(new LogoutAllResponseDTO(revoked));
    }

    private ResponseCookie buildRefreshCookie(String refreshToken) {
        return ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(Duration.ofSeconds(refreshExpirationSeconds))
                .build();
    }

    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(Duration.ZERO)
                .build();
    }

    private String resolveRefreshToken(String refreshCookie, RefreshTokenRequestDTO body) {
        if (refreshCookie != null && !refreshCookie.isBlank()) {
            return refreshCookie;
        }
        if (body != null && body.getRefreshToken() != null && !body.getRefreshToken().isBlank()) {
            return body.getRefreshToken();
        }
        return null;
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
        }
        return request.getRemoteAddr();
    }
}

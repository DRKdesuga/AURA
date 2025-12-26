package com.aura.service;

import com.aura.domain.UserEntity;
import com.aura.domain.UserRole;
import com.aura.dto.*;
import com.aura.error.AuraErrorCode;
import com.aura.error.AuraException;
import com.aura.repository.UserRepository;
import com.aura.security.AuthenticatedUser;
import com.aura.security.CurrentUserProvider;
import com.aura.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CurrentUserProvider currentUserProvider;

    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {
        String email = request.getEmail().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new AuraException(AuraErrorCode.USER_ALREADY_EXISTS, "Email already in use");
        }
        if (request.getUsername() != null && userRepository.existsByUsernameIgnoreCase(request.getUsername())) {
            throw new AuraException(AuraErrorCode.USER_ALREADY_EXISTS, "Username already in use");
        }
        UserEntity user = UserEntity.builder()
                .email(email)
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.USER)
                .enabled(true)
                .build();
        UserEntity saved = userRepository.save(user);
        return toAuthResponse(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO request) {
        UserEntity user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new AuraException(AuraErrorCode.AUTH_INVALID_CREDENTIALS, "Invalid credentials"));
        if (!user.isEnabled() || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuraException(AuraErrorCode.AUTH_INVALID_CREDENTIALS, "Invalid credentials");
        }
        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponseDTO me() {
        AuthenticatedUser principal = currentUserProvider.require();
        UserEntity user = userRepository.findById(principal.id())
                .orElseThrow(() -> new IllegalStateException("User not found: " + principal.id()));
        return toUserResponse(user);
    }

    private AuthResponseDTO toAuthResponse(UserEntity user) {
        return AuthResponseDTO.builder()
                .accessToken(jwtService.generateToken(user))
                .user(toUserResponse(user))
                .build();
    }

    private UserResponseDTO toUserResponse(UserEntity user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole().name())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}

package com.aura.security;

import com.aura.domain.UserEntity;
import com.aura.error.AuraErrorCode;
import com.aura.error.AuraException;
import com.aura.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUserProvider {

    private final UserRepository userRepository;

    public AuthenticatedUser require() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new AuraException(AuraErrorCode.FORBIDDEN, "Authentication required");
        }
        return principal;
    }

    public UserEntity requireEntity() {
        AuthenticatedUser principal = require();
        return userRepository.findById(principal.id())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + principal.id()));
    }
}

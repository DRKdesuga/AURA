package com.aura.security;

import com.aura.domain.UserRole;
import java.util.UUID;

public record AuthenticatedUser(UUID id, String email, UserRole role) {
    public boolean isAdmin() {
        return UserRole.ADMIN.equals(role);
    }
}

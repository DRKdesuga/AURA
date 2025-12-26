package com.aura.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDTO {
    private UUID id;
    private String email;
    private String username;
    private String role;
    private boolean enabled;
    private Instant createdAt;
}

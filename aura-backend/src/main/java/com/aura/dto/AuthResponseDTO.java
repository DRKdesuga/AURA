package com.aura.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDTO {
    private String accessToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private UserResponseDTO user;
}

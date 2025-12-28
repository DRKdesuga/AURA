package com.aura.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LogoutAllResponseDTO {
    private int revokedTokens;
}

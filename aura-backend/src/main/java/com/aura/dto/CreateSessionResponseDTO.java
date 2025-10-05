package com.aura.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSessionResponseDTO {
    private Long sessionId;
    private String title;
}

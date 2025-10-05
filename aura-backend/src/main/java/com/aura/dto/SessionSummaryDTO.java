package com.aura.dto;

import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionSummaryDTO {
    private Long sessionId;
    private String title;
    private String preview;
    private Instant lastMessageAt;
    private Long messageCount;
}

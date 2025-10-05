package com.aura.dto;

import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponseDTO {
    private Long sessionId;
    private Long userMessageId;
    private Long assistantMessageId;
    private String assistantReply;
    private Instant timestamp;
    private boolean newSession;
}

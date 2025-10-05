package com.aura.error;

import lombok.Builder;
import lombok.Getter;
import java.time.Instant;

@Getter
@Builder
public class ErrorResponse {
    private final String code;
    private final String message;
    private final Instant timestamp;
}

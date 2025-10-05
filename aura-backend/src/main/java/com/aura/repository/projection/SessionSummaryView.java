package com.aura.repository.projection;

import java.time.Instant;

public interface SessionSummaryView {
    Long getSessionId();
    String getTitle();
    String getPreview();
    Instant getLastMessageAt();
    Long getMessageCount();
}

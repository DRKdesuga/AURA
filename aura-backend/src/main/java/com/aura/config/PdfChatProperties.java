package com.aura.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "aura.chat.pdf")
public class PdfChatProperties {
    private long maxFileSizeMb = 25;
    private int maxPages = 200;
    private int parseTimeoutSeconds = 10;
    private int maxExtractedChars = 200000;
    private int directInjectMaxChars = 12000;
    private int chunkSizeChars = 1200;
    private int chunkOverlapChars = 120;
    private int topK = 6;
    private double minChunkScoreThreshold = 0.0;
}

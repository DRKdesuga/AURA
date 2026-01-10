package com.aura.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "aura.context")
public class AuraContextProperties {
    private int windowSizeMessages = 16;
    private int memoryUpdateEveryMessages = 10;
    private int maxPromptChars = 24000;
}

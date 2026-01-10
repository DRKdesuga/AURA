package com.aura.service;

import com.aura.domain.MessageAuthor;
import com.aura.domain.MessageEntity;

import java.util.List;

public class ContextComposer {

    public String compose(String systemPrompt,
                          String memoryJson,
                          List<MessageEntity> recentMessages,
                          String currentUserMessage) {
        StringBuilder builder = new StringBuilder();
        builder.append("[SYSTEM_PROMPT]\n")
                .append(systemPrompt == null ? "" : systemPrompt)
                .append("\n\n");

        if (memoryJson != null && !memoryJson.isBlank()) {
            builder.append("[MEMORY_JSON]\n")
                    .append(memoryJson)
                    .append("\n\n");
        }

        builder.append("[RECENT_CONVERSATION]\n");
        if (recentMessages != null) {
            for (MessageEntity message : recentMessages) {
                builder.append(labelFor(message.getAuthor()))
                        .append(": ")
                        .append(message.getContent() == null ? "" : message.getContent())
                        .append("\n");
            }
        }
        builder.append("\n[USER_MESSAGE]\n")
                .append(currentUserMessage == null ? "" : currentUserMessage)
                .append("\n\n")
                .append("[INSTRUCTIONS]\n")
                .append("Answer the user. Use MEMORY_JSON and RECENT_CONVERSATION. If unknown, ask a question.");

        return builder.toString();
    }

    private String labelFor(MessageAuthor author) {
        return (author == MessageAuthor.ASSISTANT) ? "ASSISTANT" : "USER";
    }
}

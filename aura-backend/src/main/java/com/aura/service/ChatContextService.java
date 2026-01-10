package com.aura.service;

import com.aura.config.AuraContextProperties;
import com.aura.domain.MessageAuthor;
import com.aura.domain.MessageEntity;
import com.aura.domain.SessionEntity;
import com.aura.dto.OllamaDtos.ChatMessage;
import com.aura.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatContextService {

    private static final String MEMORY_BLOCK_PREFIX = "[MEMORY_JSON]\n";

    private final MessageRepository messageRepository;
    private final AuraContextProperties contextProperties;
    private final MemoryJsonValidator memoryJsonValidator;

    public List<ChatMessage> buildContextMessages(SessionEntity session,
                                                  MessageEntity currentUserMessage,
                                                  String systemPrompt) {
        int windowSize = Math.max(0, contextProperties.getWindowSizeMessages());
        List<MessageEntity> recentMessages = fetchRecentMessages(session, currentUserMessage, windowSize);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.builder()
                .role("system")
                .content(systemPrompt == null ? "" : systemPrompt)
                .build());

        String memoryJson = session.getMemoryJson();
        if (memoryJson != null && !memoryJson.isBlank() && memoryJsonValidator.isValid(memoryJson)) {
            messages.add(ChatMessage.builder()
                    .role("system")
                    .content(MEMORY_BLOCK_PREFIX + memoryJson)
                    .build());
        }

        for (MessageEntity message : recentMessages) {
            messages.add(ChatMessage.builder()
                    .role(roleFor(message.getAuthor()))
                    .content(message.getContent())
                    .build());
        }

        messages.add(ChatMessage.builder()
                .role("user")
                .content(currentUserMessage.getContent())
                .build());

        return clampMessages(messages);
    }

    private List<MessageEntity> fetchRecentMessages(SessionEntity session,
                                                    MessageEntity currentUserMessage,
                                                    int windowSize) {
        if (windowSize <= 0) {
            return List.of();
        }
        int fetchSize = windowSize + 1;
        List<MessageEntity> recentDesc = messageRepository.findBySessionOrderByIdDesc(
                session, PageRequest.of(0, fetchSize));
        List<MessageEntity> filtered = recentDesc.stream()
                .filter(m -> currentUserMessage.getId() == null || !m.getId().equals(currentUserMessage.getId()))
                .limit(windowSize)
                .toList();
        List<MessageEntity> ordered = new ArrayList<>(filtered);
        Collections.reverse(ordered);
        return ordered;
    }

    private List<ChatMessage> clampMessages(List<ChatMessage> messages) {
        int maxPromptChars = contextProperties.getMaxPromptChars();
        if (maxPromptChars <= 0) {
            return messages;
        }
        List<ChatMessage> clamped = new ArrayList<>(messages);
        int total = totalChars(clamped);
        if (total <= maxPromptChars) {
            return clamped;
        }

        int lastIndex = clamped.size() - 1;
        int transcriptStart = memoryBlockIndex(clamped) == 1 ? 2 : 1;
        int index = transcriptStart;
        while (total > maxPromptChars && index < lastIndex) {
            ChatMessage removed = clamped.remove(index);
            total -= length(removed);
            lastIndex--;
        }

        int memoryIndex = memoryBlockIndex(clamped);
        if (total > maxPromptChars && memoryIndex == 1) {
            ChatMessage removed = clamped.remove(1);
            total -= length(removed);
            lastIndex--;
        }

        if (total > maxPromptChars && lastIndex >= 0) {
            ChatMessage last = clamped.get(lastIndex);
            int keep = Math.max(0, maxPromptChars - (total - length(last)));
            String content = last.getContent() == null ? "" : last.getContent();
            if (content.length() > keep) {
                last.setContent(content.substring(0, keep));
            }
        }
        return clamped;
    }

    private int memoryBlockIndex(List<ChatMessage> messages) {
        if (messages.size() > 1) {
            ChatMessage candidate = messages.get(1);
            if ("system".equals(candidate.getRole())
                    && candidate.getContent() != null
                    && candidate.getContent().startsWith(MEMORY_BLOCK_PREFIX)) {
                return 1;
            }
        }
        return -1;
    }

    private int totalChars(List<ChatMessage> messages) {
        int total = 0;
        for (ChatMessage message : messages) {
            total += length(message);
        }
        return total;
    }

    private int length(ChatMessage message) {
        return message.getContent() == null ? 0 : message.getContent().length();
    }

    private String roleFor(MessageAuthor author) {
        return (author == MessageAuthor.ASSISTANT) ? "assistant" : "user";
    }
}

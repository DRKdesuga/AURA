package com.aura.service;

import com.aura.client.OllamaClient;
import com.aura.config.OllamaProperties;
import com.aura.domain.MessageAuthor;
import com.aura.domain.MessageEntity;
import com.aura.domain.SessionEntity;
import com.aura.dto.ChatRequestDTO;
import com.aura.dto.ChatResponseDTO;
import com.aura.error.AuraErrorCode;
import com.aura.error.AuraException;
import com.aura.repository.MessageRepository;
import com.aura.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final SessionRepository sessionRepository;
    private final MessageRepository messageRepository;
    private final OllamaClient ollamaClient;
    private final OllamaProperties properties;

    /**
     * Creates or reuses a chat session, saves the user message, queries Ollama, saves the assistant message, and returns the result.
     */
    @Transactional
    public ChatResponseDTO chat(ChatRequestDTO request) {
        SessionEntity session = (request.getSessionId() == null)
                ? sessionRepository.save(SessionEntity.builder().build())
                : sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new AuraException(AuraErrorCode.SESSION_NOT_FOUND, "Session not found: " + request.getSessionId()));
        boolean newSession = (request.getSessionId() == null);

        MessageEntity userMsg = messageRepository.save(MessageEntity.builder()
                .author(MessageAuthor.USER)
                .content(request.getMessage())
                .session(session)
                .build());

        String answer = ollamaClient.chatOnce(request.getMessage(), properties.getSystemPrompt());

        MessageEntity botMsg = messageRepository.save(MessageEntity.builder()
                .author(MessageAuthor.ASSISTANT)
                .content(answer)
                .session(session)
                .build());

        return ChatResponseDTO.builder()
                .sessionId(session.getId())
                .userMessageId(userMsg.getId())
                .assistantMessageId(botMsg.getId())
                .assistantReply(answer)
                .timestamp(botMsg.getTimestamp())
                .newSession(newSession)
                .build();
    }
}

package com.aura.service;

import com.aura.client.OllamaClient;
import com.aura.config.OllamaProperties;
import com.aura.domain.MessageAuthor;
import com.aura.domain.MessageEntity;
import com.aura.domain.SessionEntity;
import com.aura.dto.ChatRequestDTO;
import com.aura.dto.ChatResponseDTO;
import com.aura.dto.MessageDTO;
import com.aura.error.AuraErrorCode;
import com.aura.error.AuraException;
import com.aura.repository.MessageRepository;
import com.aura.repository.SessionRepository;
import com.aura.security.AuthenticatedUser;
import com.aura.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final SessionRepository sessionRepository;
    private final MessageRepository messageRepository;
    private final OllamaClient ollamaClient;
    private final OllamaProperties properties;
    private final CurrentUserProvider currentUserProvider;

    /**
     * Creates or reuses a chat session, saves the user message, queries Ollama, saves the assistant message, and returns the result.
     */
    @Transactional
    public ChatResponseDTO chat(ChatRequestDTO request) {
        AuthenticatedUser principal = currentUserProvider.require();
        boolean newSession = (request.getSessionId() == null);

        SessionEntity session;
        if (newSession) {
            var user = currentUserProvider.requireEntity();
            session = sessionRepository.save(SessionEntity.builder()
                    .user(user)
                    .build());
        } else {
            session = resolveSession(request.getSessionId(), principal);
        }

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

    /**
     * Returns all messages for a session ordered by timestamp ascending.
     */
    @Transactional(readOnly = true)
    public List<MessageDTO> getMessages(Long sessionId) {
        AuthenticatedUser principal = currentUserProvider.require();
        SessionEntity session = resolveSession(sessionId, principal);

        return messageRepository.findBySessionOrderByTimestampAsc(session).stream()
                .map(m -> MessageDTO.builder()
                        .id(m.getId())
                        .author(m.getAuthor().name())
                        .content(m.getContent())
                        .timestamp(m.getTimestamp())
                        .build())
                .toList();
    }

    private SessionEntity resolveSession(Long sessionId, AuthenticatedUser principal) {
        return (principal.isAdmin()
                ? sessionRepository.findById(sessionId)
                : sessionRepository.findByIdAndUser_Id(sessionId, principal.id()))
                .orElseThrow(() -> new AuraException(AuraErrorCode.SESSION_NOT_FOUND, "Session not found: " + sessionId));
    }
}

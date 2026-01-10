package com.aura.service;

import com.aura.client.OllamaClient;
import com.aura.config.AuraContextProperties;
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
    private final AuraContextProperties contextProperties;
    private final ChatContextService chatContextService;
    private final MemoryUpdateService memoryUpdateService;

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

        String answer = ollamaClient.chatWithMessages(
                chatContextService.buildContextMessages(session, userMsg, properties.getSystemPrompt())
        );

        MessageEntity botMsg = messageRepository.save(MessageEntity.builder()
                .author(MessageAuthor.ASSISTANT)
                .content(answer)
                .session(session)
                .build());

        updateSessionMemoryIfNeeded(session);

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

    private void updateSessionMemoryIfNeeded(SessionEntity session) {
        int threshold = contextProperties.getMemoryUpdateEveryMessages();
        if (threshold <= 0) {
            return;
        }
        List<MessageEntity> newMessages = loadMessagesSinceLastMemory(session);
        if (newMessages.size() < threshold) {
            return;
        }
        MemoryUpdateResult result = memoryUpdateService.updateMemory(session, newMessages);
        if (result.updated()) {
            session.setMemoryJson(result.memoryJson());
            MessageEntity last = newMessages.get(newMessages.size() - 1);
            session.setLastMemoryMessageId(last.getId());
            sessionRepository.save(session);
        }
    }

    private List<MessageEntity> loadMessagesSinceLastMemory(SessionEntity session) {
        Long lastMemoryMessageId = session.getLastMemoryMessageId();
        if (lastMemoryMessageId == null) {
            return messageRepository.findBySessionOrderByIdAsc(session);
        }
        return messageRepository.findBySessionAndIdGreaterThanOrderByIdAsc(session, lastMemoryMessageId);
    }
}

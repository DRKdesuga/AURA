package com.aura.service;

import com.aura.client.OllamaClient;
import com.aura.config.OllamaProperties;
import com.aura.domain.MessageAuthor;
import com.aura.domain.MessageEntity;
import com.aura.domain.SessionEntity;
import com.aura.domain.UserEntity;
import com.aura.domain.UserRole;
import com.aura.dto.ChatRequestDTO;
import com.aura.dto.ChatResponseDTO;
import com.aura.dto.MessageDTO;
import com.aura.error.AuraErrorCode;
import com.aura.error.AuraException;
import com.aura.repository.MessageRepository;
import com.aura.repository.SessionRepository;
import com.aura.security.AuthenticatedUser;
import com.aura.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock SessionRepository sessionRepository;
    @Mock MessageRepository messageRepository;
    @Mock OllamaClient ollamaClient;
    @Mock OllamaProperties properties;
    @Mock CurrentUserProvider currentUserProvider;

    @InjectMocks ChatService chatService;

    AtomicLong idGen;
    UserEntity user;
    AuthenticatedUser principal;

    @BeforeEach
    void setUp() {
        idGen = new AtomicLong(1L);
        UUID userId = UUID.randomUUID();
        user = UserEntity.builder().id(userId).email("user@aura.local").role(UserRole.USER).build();
        principal = new AuthenticatedUser(userId, user.getEmail(), UserRole.USER);
        when(currentUserProvider.require()).thenReturn(principal);
    }

    /**
     * Verifies that a new session is created when none is provided, the user message is saved,
     * Ollama is called with the system prompt, the assistant message is saved, and a proper DTO is returned.
     */
    @Test
    @DisplayName("chat: creates new session and returns assistant reply")
    void chat_createsNewSession_andReturnsAssistantReply() {
        SessionEntity persistedSession = SessionEntity.builder().id(42L).user(user).build();
        when(currentUserProvider.requireEntity()).thenReturn(user);
        when(sessionRepository.save(any(SessionEntity.class))).thenReturn(persistedSession);

        when(properties.getSystemPrompt()).thenReturn("SYS");
        when(ollamaClient.chatOnce(eq("Hi"), eq("SYS"))).thenReturn("Hello from Ollama");

        when(messageRepository.save(any(MessageEntity.class))).thenAnswer(inv -> {
            MessageEntity in = inv.getArgument(0);
            return MessageEntity.builder()
                    .id(idGen.getAndIncrement())
                    .author(in.getAuthor())
                    .content(in.getContent())
                    .timestamp(in.getTimestamp() != null ? in.getTimestamp() : Instant.now())
                    .session(in.getSession())
                    .build();
        });

        ChatRequestDTO req = ChatRequestDTO.builder().sessionId(null).message("Hi").build();
        ChatResponseDTO out = chatService.chat(req);

        assertThat(out.isNewSession()).isTrue();
        assertThat(out.getSessionId()).isEqualTo(42L);
        assertThat(out.getAssistantReply()).isEqualTo("Hello from Ollama");
        assertThat(out.getUserMessageId()).isNotNull();
        assertThat(out.getAssistantMessageId()).isNotNull();
        assertThat(out.getTimestamp()).isNotNull();

        InOrder order = inOrder(sessionRepository, messageRepository, ollamaClient, messageRepository);
        order.verify(sessionRepository).save(any(SessionEntity.class));
        order.verify(messageRepository).save(argThat(m -> m.getAuthor() == MessageAuthor.USER && "Hi".equals(m.getContent())));
        order.verify(ollamaClient).chatOnce("Hi", "SYS");
        order.verify(messageRepository).save(argThat(m -> m.getAuthor() == MessageAuthor.ASSISTANT && "Hello from Ollama".equals(m.getContent())));
    }

    /**
     * Verifies that the existing session is reused when sessionId is provided and no new session is created.
     */
    @Test
    @DisplayName("chat: reuses existing session when sessionId provided")
    void chat_usesExistingSession_whenSessionIdProvided() {
        SessionEntity existing = SessionEntity.builder().id(7L).user(user).build();
        when(sessionRepository.findByIdAndUser_Id(7L, user.getId())).thenReturn(Optional.of(existing));

        when(properties.getSystemPrompt()).thenReturn("SYS2");
        when(ollamaClient.chatOnce(eq("Second"), eq("SYS2"))).thenReturn("Reply 2");

        when(messageRepository.save(any(MessageEntity.class))).thenAnswer(inv -> {
            MessageEntity in = inv.getArgument(0);
            return MessageEntity.builder()
                    .id(idGen.getAndIncrement())
                    .author(in.getAuthor())
                    .content(in.getContent())
                    .timestamp(Instant.now())
                    .session(in.getSession())
                    .build();
        });

        ChatRequestDTO req = ChatRequestDTO.builder().sessionId(7L).message("Second").build();
        ChatResponseDTO out = chatService.chat(req);

        assertThat(out.isNewSession()).isFalse();
        assertThat(out.getSessionId()).isEqualTo(7L);
        assertThat(out.getAssistantReply()).isEqualTo("Reply 2");
        verify(sessionRepository, never()).save(any());
        verify(sessionRepository).findByIdAndUser_Id(7L, user.getId());
    }

    /**
     * Verifies that an AuraException with SESSION_NOT_FOUND is thrown when the session does not exist.
     */
    @Test
    @DisplayName("chat: throws when session not found")
    void chat_throws_whenSessionNotFound() {
        when(sessionRepository.findByIdAndUser_Id(eq(99L), any(UUID.class))).thenReturn(Optional.empty());
        ChatRequestDTO req = ChatRequestDTO.builder().sessionId(99L).message("X").build();

        assertThatThrownBy(() -> chatService.chat(req))
                .isInstanceOf(AuraException.class)
                .hasMessageContaining("Session not found: 99")
                .extracting("code").isEqualTo(AuraErrorCode.SESSION_NOT_FOUND);

        verify(messageRepository, never()).save(any());
        verify(ollamaClient, never()).chatOnce(anyString(), anyString());
    }

    /**
     * Verifies that the service maps entities to DTOs and returns messages ordered by timestamp ascending.
     */
    @Test
    @DisplayName("getMessages: returns mapped DTOs ordered by timestamp")
    void getMessages_returnsMappedDtos() {
        SessionEntity s = SessionEntity.builder().id(5L).user(user).build();
        when(sessionRepository.findByIdAndUser_Id(eq(5L), any(UUID.class))).thenReturn(Optional.of(s));

        MessageEntity m1 = MessageEntity.builder()
                .id(1L).author(MessageAuthor.USER).content("Hello").timestamp(Instant.parse("2024-01-01T00:00:00Z")).session(s).build();
        MessageEntity m2 = MessageEntity.builder()
                .id(2L).author(MessageAuthor.ASSISTANT).content("Hi there").timestamp(Instant.parse("2024-01-01T00:00:01Z")).session(s).build();

        when(messageRepository.findBySessionOrderByTimestampAsc(s)).thenReturn(List.of(m1, m2));

        List<MessageDTO> dtos = chatService.getMessages(5L);

        assertThat(dtos).hasSize(2);
        assertThat(dtos.getFirst().getId()).isEqualTo(1L);
        assertThat(dtos.get(0).getAuthor()).isEqualTo("USER");
        assertThat(dtos.get(0).getContent()).isEqualTo("Hello");
        assertThat(dtos.get(1).getAuthor()).isEqualTo("ASSISTANT");
    }

    /**
     * Verifies that an AuraException with SESSION_NOT_FOUND is thrown when reading messages for a missing session.
     */
    @Test
    @DisplayName("getMessages: throws when session not found")
    void getMessages_throws_whenSessionNotFound() {
        when(sessionRepository.findByIdAndUser_Id(eq(123L), any(UUID.class))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> chatService.getMessages(123L))
                .isInstanceOf(AuraException.class)
                .extracting("code").isEqualTo(AuraErrorCode.SESSION_NOT_FOUND);
    }

    /**
     * Verifies that exceptions from Ollama are propagated and that only the user message has been saved before the failure.
     */
    @Test
    @DisplayName("chat: propagates Ollama failure and saves only the user message")
    void chat_propagatesException_whenOllamaFails() {
        SessionEntity s = SessionEntity.builder().id(10L).user(user).build();
        when(currentUserProvider.requireEntity()).thenReturn(user);
        when(sessionRepository.save(any(SessionEntity.class))).thenReturn(s);

        when(properties.getSystemPrompt()).thenReturn("SYS");
        when(ollamaClient.chatOnce(anyString(), anyString()))
                .thenThrow(new AuraException(AuraErrorCode.OLLAMA_UNREACHABLE, "down"));

        when(messageRepository.save(any(MessageEntity.class))).thenAnswer(inv -> {
            MessageEntity in = inv.getArgument(0);
            return MessageEntity.builder()
                    .id(idGen.getAndIncrement())
                    .author(in.getAuthor())
                    .content(in.getContent())
                    .timestamp(Instant.now())
                    .session(in.getSession())
                    .build();
        });

        ChatRequestDTO req = ChatRequestDTO.builder().sessionId(null).message("Ping").build();

        assertThatThrownBy(() -> chatService.chat(req))
                .isInstanceOf(AuraException.class)
                .extracting("code").isEqualTo(AuraErrorCode.OLLAMA_UNREACHABLE);

        ArgumentCaptor<MessageEntity> captor = ArgumentCaptor.forClass(MessageEntity.class);
        verify(messageRepository, times(1)).save(captor.capture());
        MessageEntity saved = captor.getValue();
        assertThat(saved.getAuthor()).isEqualTo(MessageAuthor.USER);
        verify(messageRepository, never()).save(argThat(m -> m.getAuthor() == MessageAuthor.ASSISTANT));
    }
}

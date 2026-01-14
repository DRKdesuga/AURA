package com.aura.service;

import com.aura.client.OllamaClient;
import com.aura.config.AuraContextProperties;
import com.aura.config.OllamaProperties;
import com.aura.config.PdfChatProperties;
import com.aura.domain.MessageAuthor;
import com.aura.domain.MessageEntity;
import com.aura.domain.SessionEntity;
import com.aura.domain.UserEntity;
import com.aura.domain.UserRole;
import com.aura.dto.ChatRequestDTO;
import com.aura.dto.ChatResponseDTO;
import com.aura.dto.MessageDTO;
import com.aura.dto.OllamaDtos.ChatMessage;
import com.aura.error.AuraErrorCode;
import com.aura.error.AuraException;
import com.aura.repository.MessageRepository;
import com.aura.repository.SessionRepository;
import com.aura.security.AuthenticatedUser;
import com.aura.security.CurrentUserProvider;
import com.aura.service.pdf.LexicalRetriever;
import com.aura.service.pdf.PdfPromptBuilder;
import com.aura.service.pdf.PdfTextExtractor;
import com.aura.service.pdf.ScoredChunk;
import com.aura.service.pdf.TextChunk;
import com.aura.service.pdf.TextChunker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.ArrayList;
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
    @Mock AuraContextProperties contextProperties;
    @Mock ChatContextService chatContextService;
    @Mock MemoryUpdateService memoryUpdateService;
    @Mock PdfTextExtractor pdfTextExtractor;
    @Mock TextChunker textChunker;
    @Mock LexicalRetriever lexicalRetriever;
    @Spy PdfPromptBuilder pdfPromptBuilder = new PdfPromptBuilder();
    @Spy PdfChatProperties pdfChatProperties = new PdfChatProperties();

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
        pdfChatProperties.setDirectInjectMaxChars(12000);
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
        List<ChatMessage> contextMessages = List.of(
                ChatMessage.builder().role("system").content("SYS").build(),
                ChatMessage.builder().role("user").content("Hi").build()
        );
        when(chatContextService.buildContextMessages(eq(persistedSession), any(MessageEntity.class), eq("SYS")))
                .thenReturn(contextMessages);
        when(ollamaClient.chatWithMessages(contextMessages)).thenReturn("Hello from Ollama");

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
        order.verify(ollamaClient).chatWithMessages(contextMessages);
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
        List<ChatMessage> contextMessages = List.of(
                ChatMessage.builder().role("system").content("SYS2").build(),
                ChatMessage.builder().role("user").content("Second").build()
        );
        when(chatContextService.buildContextMessages(eq(existing), any(MessageEntity.class), eq("SYS2")))
                .thenReturn(contextMessages);
        when(ollamaClient.chatWithMessages(contextMessages)).thenReturn("Reply 2");

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
        verify(ollamaClient, never()).chatWithMessages(anyList());
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
        List<ChatMessage> contextMessages = List.of(
                ChatMessage.builder().role("system").content("SYS").build(),
                ChatMessage.builder().role("user").content("Ping").build()
        );
        when(chatContextService.buildContextMessages(eq(s), any(MessageEntity.class), eq("SYS")))
                .thenReturn(contextMessages);
        when(ollamaClient.chatWithMessages(anyList()))
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

    /**
     * Verifies that memory is updated when enough new messages have accumulated.
     */
    @Test
    @DisplayName("chat: updates memory when threshold reached")
    void chat_updatesMemory_whenThresholdReached() {
        SessionEntity persistedSession = SessionEntity.builder().id(44L).user(user).build();
        when(currentUserProvider.requireEntity()).thenReturn(user);
        when(sessionRepository.save(any(SessionEntity.class))).thenReturn(persistedSession);
        when(contextProperties.getMemoryUpdateEveryMessages()).thenReturn(2);

        when(properties.getSystemPrompt()).thenReturn("SYS");
        List<ChatMessage> contextMessages = List.of(
                ChatMessage.builder().role("system").content("SYS").build(),
                ChatMessage.builder().role("user").content("Hi").build()
        );
        when(chatContextService.buildContextMessages(eq(persistedSession), any(MessageEntity.class), eq("SYS")))
                .thenReturn(contextMessages);
        when(ollamaClient.chatWithMessages(contextMessages)).thenReturn("Hello");

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

        List<MessageEntity> messagesForMemory = List.of(
                MessageEntity.builder().id(1L).author(MessageAuthor.USER).content("Hi").session(persistedSession).build(),
                MessageEntity.builder().id(2L).author(MessageAuthor.ASSISTANT).content("Hello").session(persistedSession).build()
        );
        when(messageRepository.findBySessionOrderByIdAsc(persistedSession)).thenReturn(messagesForMemory);
        when(memoryUpdateService.updateMemory(persistedSession, messagesForMemory))
                .thenReturn(MemoryUpdateResult.updated("{\"facts\":[\"x\"]}"));

        ChatRequestDTO req = ChatRequestDTO.builder().sessionId(null).message("Hi").build();
        chatService.chat(req);

        assertThat(persistedSession.getMemoryJson()).isEqualTo("{\"facts\":[\"x\"]}");
        assertThat(persistedSession.getLastMemoryMessageId()).isEqualTo(2L);
        verify(memoryUpdateService).updateMemory(persistedSession, messagesForMemory);
    }

    /**
     * Verifies that small extracted text is injected directly into the prompt with untrusted instructions.
     */
    @Test
    @DisplayName("chatWithFile: direct injects small extracted text")
    void chatWithFile_directInjectsSmallText() {
        SessionEntity persistedSession = SessionEntity.builder().id(101L).user(user).build();
        when(currentUserProvider.requireEntity()).thenReturn(user);
        when(sessionRepository.save(any(SessionEntity.class))).thenReturn(persistedSession);
        when(properties.getSystemPrompt()).thenReturn("SYS");

        String extractedText = "Small PDF content.";
        when(pdfTextExtractor.extractText(any())).thenReturn(extractedText);

        List<ChatMessage> contextMessages = new ArrayList<>(List.of(
                ChatMessage.builder().role("system").content("SYS").build(),
                ChatMessage.builder().role("user").content("placeholder").build()
        ));
        when(chatContextService.buildContextMessages(eq(persistedSession), any(MessageEntity.class), eq("SYS")))
                .thenReturn(contextMessages);

        when(ollamaClient.chatWithMessages(anyList())).thenReturn("Answer");
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

        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", MediaType.APPLICATION_PDF_VALUE, "pdf".getBytes());
        ChatResponseDTO out = chatService.chatWithFile(null, "Summarize this", file);

        ArgumentCaptor<List<ChatMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(ollamaClient).chatWithMessages(captor.capture());
        String prompt = captor.getValue().getLast().getContent();
        assertThat(prompt).contains("untrusted");
        assertThat(prompt).contains(extractedText);
        assertThat(prompt).contains("Summarize this");
        assertThat(out.getAssistantReply()).isEqualTo("Answer");
        verify(messageRepository).save(argThat(m -> m.getAuthor() == MessageAuthor.USER));
        verify(messageRepository).save(argThat(m -> m.getAuthor() == MessageAuthor.ASSISTANT));
    }

    /**
     * Verifies that large extracted text uses retrieval and only selected chunks appear in the prompt.
     */
    @Test
    @DisplayName("chatWithFile: uses top chunks for large text")
    void chatWithFile_usesTopChunksForLargeText() {
        pdfChatProperties.setDirectInjectMaxChars(5);
        pdfChatProperties.setTopK(1);

        SessionEntity existing = SessionEntity.builder().id(55L).user(user).build();
        when(sessionRepository.findByIdAndUser_Id(55L, user.getId())).thenReturn(Optional.of(existing));
        when(properties.getSystemPrompt()).thenReturn("SYS");

        String extractedText = "FULL_TEXT_SHOULD_NOT_APPEAR";
        when(pdfTextExtractor.extractText(any())).thenReturn(extractedText);

        List<TextChunk> chunks = List.of(
                new TextChunk(0, "CHUNK_ALPHA"),
                new TextChunk(1, "CHUNK_BETA")
        );
        when(textChunker.chunk(eq(extractedText), anyInt(), anyInt())).thenReturn(chunks);
        List<ScoredChunk> selected = List.of(new ScoredChunk(chunks.get(1), 1.5));
        when(lexicalRetriever.retrieveTopChunks(eq("Find beta"), eq(chunks), eq(1), anyDouble()))
                .thenReturn(selected);

        List<ChatMessage> contextMessages = new ArrayList<>(List.of(
                ChatMessage.builder().role("system").content("SYS").build(),
                ChatMessage.builder().role("user").content("placeholder").build()
        ));
        when(chatContextService.buildContextMessages(eq(existing), any(MessageEntity.class), eq("SYS")))
                .thenReturn(contextMessages);

        when(ollamaClient.chatWithMessages(anyList())).thenReturn("Done");
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

        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", MediaType.APPLICATION_PDF_VALUE, "pdf".getBytes());
        chatService.chatWithFile(55L, "Find beta", file);

        ArgumentCaptor<List<ChatMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(ollamaClient).chatWithMessages(captor.capture());
        String prompt = captor.getValue().getLast().getContent();
        assertThat(prompt).contains("CHUNK_BETA");
        assertThat(prompt).doesNotContain("CHUNK_ALPHA");
        assertThat(prompt).doesNotContain(extractedText);
        assertThat(prompt).contains("untrusted");
    }

    /**
     * Verifies that invalid file types propagate as domain exceptions.
     */
    @Test
    @DisplayName("chatWithFile: throws on invalid file type")
    void chatWithFile_throwsOnInvalidFileType() {
        SessionEntity existing = SessionEntity.builder().id(88L).user(user).build();
        when(sessionRepository.findByIdAndUser_Id(88L, user.getId())).thenReturn(Optional.of(existing));
        when(pdfTextExtractor.extractText(any())).thenThrow(new AuraException(
                AuraErrorCode.INVALID_FILE_TYPE, "Only PDF files are supported"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.txt", MediaType.TEXT_PLAIN_VALUE, "text".getBytes());

        assertThatThrownBy(() -> chatService.chatWithFile(88L, "Hello", file))
                .isInstanceOf(AuraException.class)
                .extracting("code").isEqualTo(AuraErrorCode.INVALID_FILE_TYPE);
    }
}

package com.aura.service;

import com.aura.client.OllamaClient;
import com.aura.domain.MessageAuthor;
import com.aura.domain.MessageEntity;
import com.aura.domain.SessionEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemoryUpdateServiceTest {

    @Mock
    OllamaClient ollamaClient;

    @Test
    @DisplayName("updateMemory: keeps previous memory when JSON is invalid")
    void updateMemory_keepsOldMemory_onInvalidJson() {
        MemoryJsonValidator validator = new MemoryJsonValidator(new ObjectMapper());
        MemoryUpdateService service = new MemoryUpdateService(ollamaClient, validator);
        SessionEntity session = SessionEntity.builder()
                .memoryJson("{\"facts\":[\"known\"]}")
                .build();

        List<MessageEntity> messages = List.of(
                MessageEntity.builder().author(MessageAuthor.USER).content("Remember this").build()
        );

        when(ollamaClient.chatWithMessages(anyList())).thenReturn("not-json");

        MemoryUpdateResult result = service.updateMemory(session, messages);

        assertThat(result.updated()).isFalse();
        assertThat(result.memoryJson()).isEqualTo("{\"facts\":[\"known\"]}");
    }
}

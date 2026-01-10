package com.aura.client;

import com.aura.config.OllamaProperties;
import com.aura.dto.OllamaDtos.ChatMessage;
import com.aura.dto.OllamaDtos.ChatRequest;
import com.aura.dto.OllamaDtos.ChatResponse;
import com.aura.error.AuraErrorCode;
import com.aura.error.AuraException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OllamaClient {

    private final RestClient ollamaRestClient;
    private final OllamaProperties props;

    /**
     * Sends a single-turn chat request to Ollama and returns the assistant reply.
     */
    public String chatOnce(String userContent, String systemPrompt) {
        ChatRequest body = ChatRequest.builder()
                .model(props.getModel())
                .stream(false)
                .messages(List.of(
                        ChatMessage.builder().role("system").content(systemPrompt).build(),
                        ChatMessage.builder().role("user").content(userContent).build()
                ))
                .build();

        ChatResponse resp = ollamaRestClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(ChatResponse.class);

        if (resp == null || resp.getMessage() == null || resp.getMessage().getContent() == null) {
            throw new AuraException(AuraErrorCode.OLLAMA_EMPTY_RESPONSE, "Empty response from Ollama");
        }
        return resp.getMessage().getContent();
    }

    /**
     * Sends a multi-turn chat request to Ollama and returns the assistant reply.
     */
    public String chatWithMessages(List<ChatMessage> messages) {
        ChatRequest body = ChatRequest.builder()
                .model(props.getModel())
                .stream(false)
                .messages(messages)
                .build();

        ChatResponse resp = ollamaRestClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(ChatResponse.class);

        if (resp == null || resp.getMessage() == null || resp.getMessage().getContent() == null) {
            throw new AuraException(AuraErrorCode.OLLAMA_EMPTY_RESPONSE, "Empty response from Ollama");
        }
        return resp.getMessage().getContent();
    }
}

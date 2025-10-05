package com.aura.controller;

import com.aura.dto.ChatRequestDTO;
import com.aura.dto.ChatResponseDTO;
import com.aura.repository.SessionRepository;
import com.aura.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin
public class ChatController {

    private final ChatService chatService;
    private final SessionRepository sessionRepository;

    /**
     * Handles a chat request and returns the assistant reply with session context.
     */
    @PostMapping
    public ResponseEntity<ChatResponseDTO> chat(@Valid @RequestBody ChatRequestDTO request) {
        return ResponseEntity.ok(chatService.chat(request));
    }

    /**
     * Returns whether a session exists.
     */
    @GetMapping("/session/{id}/exists")
    public ResponseEntity<Boolean> sessionExists(@PathVariable Long id) {
        return ResponseEntity.ok(sessionRepository.existsById(id));
    }
}

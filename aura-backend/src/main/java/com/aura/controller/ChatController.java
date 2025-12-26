package com.aura.controller;

import com.aura.dto.ChatRequestDTO;
import com.aura.dto.ChatResponseDTO;
import com.aura.dto.MessageDTO;
import com.aura.repository.SessionRepository;
import com.aura.security.CurrentUserProvider;
import com.aura.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin
public class ChatController {

    private final ChatService chatService;
    private final SessionRepository sessionRepository;
    private final CurrentUserProvider currentUserProvider;

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
        var principal = currentUserProvider.require();
        boolean exists = principal.isAdmin()
                ? sessionRepository.existsById(id)
                : sessionRepository.findByIdAndUser_Id(id, principal.id()).isPresent();
        return ResponseEntity.ok(exists);
    }

    /**
     * Returns all messages for a session ordered by timestamp ascending.
     */
    @GetMapping("/session/{id}/messages")
    public ResponseEntity<List<MessageDTO>> getMessages(@PathVariable Long id) {
        return ResponseEntity.ok(chatService.getMessages(id));
    }
}

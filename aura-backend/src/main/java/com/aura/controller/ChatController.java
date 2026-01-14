package com.aura.controller;

import com.aura.dto.ChatRequestDTO;
import com.aura.dto.ChatResponseDTO;
import com.aura.dto.MessageDTO;
import com.aura.repository.SessionRepository;
import com.aura.security.CurrentUserProvider;
import com.aura.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
     * Handles a chat request with an optional PDF upload and returns the assistant reply.
     */
    @PostMapping(path = "/with-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ChatResponseDTO> chatWithFile(@RequestPart("message") String message,
                                                        @RequestPart(value = "sessionId", required = false) String sessionId,
                                                        @RequestPart(value = "file", required = false) MultipartFile file) {
        Long parsedSessionId = parseSessionId(sessionId);
        return ResponseEntity.ok(chatService.chatWithFile(parsedSessionId, message, file));
    }

    private Long parseSessionId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException ex) {
            throw new com.aura.error.AuraException(com.aura.error.AuraErrorCode.VALIDATION_ERROR,
                    "sessionId must be a number");
        }
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

package com.aura.controller;

import com.aura.dto.CreateSessionResponseDTO;
import com.aura.dto.SessionsPageDTO;
import com.aura.dto.UpdateSessionTitleRequestDTO;
import com.aura.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@CrossOrigin
public class SessionController {

    private final SessionService sessionService;

    /**
     * Returns paged session summaries filtered by query.
     */
    @GetMapping
    public ResponseEntity<SessionsPageDTO> list(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(sessionService.list(q, page, size));
    }

    /**
     * Creates a new empty session.
     */
    @PostMapping
    public ResponseEntity<CreateSessionResponseDTO> create(
            @RequestParam(name = "title", required = false) String title
    ) {
        return ResponseEntity.ok(sessionService.create(title));
    }

    /**
     * Updates the session title.
     */
    @PatchMapping("/{id}/title")
    public ResponseEntity<Void> updateTitle(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSessionTitleRequestDTO body
    ) {
        sessionService.updateTitle(id, body);
        return ResponseEntity.noContent().build();
    }
}

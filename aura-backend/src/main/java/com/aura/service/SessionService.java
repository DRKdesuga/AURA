package com.aura.service;

import com.aura.domain.SessionEntity;
import com.aura.dto.CreateSessionResponseDTO;
import com.aura.dto.SessionSummaryDTO;
import com.aura.dto.SessionsPageDTO;
import com.aura.dto.UpdateSessionTitleRequestDTO;
import com.aura.error.AuraErrorCode;
import com.aura.error.AuraException;
import com.aura.repository.SessionRepository;
import com.aura.repository.projection.SessionSummaryView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;

    /**
     * Returns paged session summaries filtered by query.
     */
    @Transactional(readOnly = true)
    public SessionsPageDTO list(String query, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<SessionSummaryView> p = sessionRepository.searchSummaries(query, pageable);
        List<SessionSummaryDTO> items = p.getContent().stream()
                .map(v -> SessionSummaryDTO.builder()
                        .sessionId(v.getSessionId())
                        .title(v.getTitle())
                        .preview(v.getPreview())
                        .lastMessageAt(v.getLastMessageAt())
                        .messageCount(v.getMessageCount())
                        .build())
                .toList();
        return SessionsPageDTO.builder()
                .items(items)
                .total(p.getTotalElements())
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .build();
    }

    /**
     * Creates a new empty session with optional title.
     */
    @Transactional
    public CreateSessionResponseDTO create(String title) {
        SessionEntity s = SessionEntity.builder()
                .title(title)
                .build();
        SessionEntity saved = sessionRepository.save(s);
        return CreateSessionResponseDTO.builder()
                .sessionId(saved.getId())
                .title(saved.getTitle())
                .build();
    }

    /**
     * Updates the session title.
     */
    @Transactional
    public void updateTitle(Long id, UpdateSessionTitleRequestDTO body) {
        SessionEntity s = sessionRepository.findById(id)
                .orElseThrow(() -> new AuraException(AuraErrorCode.SESSION_NOT_FOUND, "Session not found: " + id));
        s.setTitle(body.getTitle());
        sessionRepository.save(s);
    }
}

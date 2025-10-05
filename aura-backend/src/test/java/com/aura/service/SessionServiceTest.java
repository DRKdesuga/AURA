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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    SessionRepository sessionRepository;

    @InjectMocks
    SessionService sessionService;

    private SessionSummaryView view(long id, String title, String preview, Instant last, long count) {
        return new SessionSummaryView() {
            public Long getSessionId() { return id; }
            public String getTitle() { return title; }
            public String getPreview() { return preview; }
            public Instant getLastMessageAt() { return last; }
            public Long getMessageCount() { return count; }
        };
    }

    /**
     * Verifies list() maps repository projection to DTOs and preserves pagination metadata.
     */
    @Test
    @DisplayName("list: returns mapped items with pagination")
    void list_returnsMappedItems() {
        var pageable = PageRequest.of(0, 2);
        var now = Instant.parse("2025-01-01T00:00:00Z");
        Page<SessionSummaryView> page = new PageImpl<>(
                List.of(
                        view(10L, "First", "Hi", now, 2L),
                        view(9L, "Second", "Hello", now.minusSeconds(60), 5L)
                ),
                pageable,
                5
        );
        when(sessionRepository.searchSummaries(eq("hi"), eq(pageable))).thenReturn(page);

        SessionsPageDTO out = sessionService.list("hi", 0, 2);

        assertThat(out.getTotal()).isEqualTo(5);
        assertThat(out.getPage()).isEqualTo(0);
        assertThat(out.getSize()).isEqualTo(2);
        assertThat(out.getItems()).hasSize(2);
        SessionSummaryDTO first = out.getItems().getFirst();
        assertThat(first.getSessionId()).isEqualTo(10L);
        assertThat(first.getTitle()).isEqualTo("First");
        assertThat(first.getPreview()).isEqualTo("Hi");
        assertThat(first.getLastMessageAt()).isEqualTo(now);
        assertThat(first.getMessageCount()).isEqualTo(2L);
        verify(sessionRepository).searchSummaries(eq("hi"), eq(pageable));
    }

    /**
     * Verifies create() persists a new empty session with an optional title.
     */
    @Test
    @DisplayName("create: persists new session and returns its id")
    void create_persistsSession() {
        SessionEntity saved = SessionEntity.builder().id(77L).title("Notes").build();
        when(sessionRepository.save(any(SessionEntity.class))).thenReturn(saved);

        CreateSessionResponseDTO out = sessionService.create("Notes");

        assertThat(out.getSessionId()).isEqualTo(77L);
        assertThat(out.getTitle()).isEqualTo("Notes");
        verify(sessionRepository).save(argThat(s -> "Notes".equals(s.getTitle())));
    }

    /**
     * Verifies updateTitle() saves the new title for an existing session.
     */
    @Test
    @DisplayName("updateTitle: updates title on existing session")
    void updateTitle_updatesTitle() {
        SessionEntity existing = SessionEntity.builder().id(5L).title("Old").build();
        when(sessionRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(sessionRepository.save(any(SessionEntity.class))).thenAnswer(i -> i.getArgument(0));

        sessionService.updateTitle(5L, UpdateSessionTitleRequestDTO.builder().title("New").build());

        assertThat(existing.getTitle()).isEqualTo("New");
        verify(sessionRepository).save(existing);
    }

    /**
     * Verifies updateTitle() throws a domain exception when the session does not exist.
     */
    @Test
    @DisplayName("updateTitle: throws when session not found")
    void updateTitle_throwsWhenNotFound() {
        when(sessionRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.updateTitle(404L,
                UpdateSessionTitleRequestDTO.builder().title("X").build()))
                .isInstanceOf(AuraException.class)
                .extracting("code").isEqualTo(AuraErrorCode.SESSION_NOT_FOUND);

        verify(sessionRepository, never()).save(any());
    }
}

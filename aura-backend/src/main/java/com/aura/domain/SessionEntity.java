package com.aura.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "chat_sessions")
public class SessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "date_debut", nullable = false)
    private Instant startDate;

    @Column(name = "date_fin")
    private Instant endDate;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "memory_json", columnDefinition = "TEXT")
    private String memoryJson;

    @Column(name = "summary_text", columnDefinition = "TEXT")
    private String summaryText;

    @Column(name = "last_memory_message_id")
    private Long lastMemoryMessageId;

    @PrePersist
    void onCreate() {
        if (startDate == null) startDate = Instant.now();
    }
}

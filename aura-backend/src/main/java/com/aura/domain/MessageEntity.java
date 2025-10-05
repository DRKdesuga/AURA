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
@Table(name = "messages")
public class MessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "auteur", nullable = false, length = 16)
    private MessageAuthor author;

    @Column(name = "contenu", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "horodatage", nullable = false)
    private Instant timestamp;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private SessionEntity session;

    @PrePersist
    void onCreate() {
        if (timestamp == null) timestamp = Instant.now();
    }
}

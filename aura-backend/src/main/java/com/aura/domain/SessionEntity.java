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
@Table(name = "sessions")
public class SessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long id;

    @Column(name = "date_debut", nullable = false)
    private Instant startDate;

    @Column(name = "date_fin")
    private Instant endDate;

    @Column(name = "title", length = 200)
    private String title;

    @PrePersist
    void onCreate() {
        if (startDate == null) startDate = Instant.now();
    }
}

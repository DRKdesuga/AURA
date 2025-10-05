package com.aura.repository;

import com.aura.domain.SessionEntity;
import com.aura.repository.projection.SessionSummaryView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface SessionRepository extends JpaRepository<SessionEntity, Long> {

    @Query(
            value = """
        SELECT
          s.session_id AS "sessionId",
          COALESCE(s.title, fu.first_user_content, '') AS "title",
          lm.last_content AS "preview",
          lm.last_time AS "lastMessageAt",
          lm.message_count AS "messageCount"
        FROM sessions s
        LEFT JOIN LATERAL (
          SELECT m.contenu AS first_user_content
          FROM messages m
          WHERE m.session_id = s.session_id AND m.auteur = 'USER'
          ORDER BY m.horodatage ASC
          LIMIT 1
        ) fu ON true
        LEFT JOIN LATERAL (
          SELECT COUNT(*)::bigint AS message_count,
                 MAX(m.horodatage) AS last_time,
                 (
                   SELECT m2.contenu
                   FROM messages m2
                   WHERE m2.session_id = s.session_id
                   ORDER BY m2.horodatage DESC
                   LIMIT 1
                 ) AS last_content
          FROM messages m
          WHERE m.session_id = s.session_id
        ) lm ON true
        WHERE (:q IS NULL OR :q = '' OR
               LOWER(COALESCE(s.title, fu.first_user_content, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
               EXISTS (
                 SELECT 1 FROM messages m3
                 WHERE m3.session_id = s.session_id
                   AND m3.contenu ILIKE CONCAT('%', :q, '%')
               ))
        ORDER BY lm.last_time DESC NULLS LAST, s.session_id DESC
        """,
            countQuery = """
        SELECT COUNT(*)::bigint
        FROM sessions s
        LEFT JOIN LATERAL (
          SELECT m.contenu AS first_user_content
          FROM messages m
          WHERE m.session_id = s.session_id AND m.auteur = 'USER'
          ORDER BY m.horodatage ASC
          LIMIT 1
        ) fu ON true
        WHERE (:q IS NULL OR :q = '' OR
               LOWER(COALESCE(s.title, fu.first_user_content, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
               EXISTS (
                 SELECT 1 FROM messages m3
                 WHERE m3.session_id = s.session_id
                   AND m3.contenu ILIKE CONCAT('%', :q, '%')
               ))
        """,
            nativeQuery = true
    )
    Page<SessionSummaryView> searchSummaries(@Param("q") String query, Pageable pageable);
}

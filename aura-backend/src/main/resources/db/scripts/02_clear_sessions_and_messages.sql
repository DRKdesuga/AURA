-- Preview: list every session that will be removed, with counts and last message timestamp
SELECT
    s.session_id,
    COALESCE(s.title, '(no title)') AS title,
    s.date_debut                     AS start_date,
    s.date_fin                       AS end_date,
    COUNT(m.message_id)              AS message_count,
    MAX(m.horodatage)                AS last_message_at
FROM chat_sessions s
LEFT JOIN chat_messages m ON m.session_id = s.session_id
GROUP BY s.session_id, s.title, s.date_debut, s.date_fin
ORDER BY s.session_id DESC;

-- Dangerous: removes all sessions and their messages, resets identities
TRUNCATE TABLE chat_sessions RESTART IDENTITY CASCADE;

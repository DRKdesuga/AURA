--List sessions with message count and last message timestamp
SELECT
    s.session_id,
    s.date_debut      AS start_date,
    s.date_fin        AS end_date,
    COUNT(m.message_id) AS message_count,
    MAX(m.horodatage) AS last_message_at
FROM sessions s
         LEFT JOIN messages m ON m.session_id = s.session_id
GROUP BY s.session_id, s.date_debut, s.date_fin
ORDER BY s.session_id DESC;

SELECT
    m.message_id   AS id,
    m.session_id,
    m.auteur       AS author,
    m.contenu      AS content,
    m.horodatage   AS timestamp
FROM messages m
WHERE m.session_id = :id
ORDER BY m.horodatage ASC;

--Latest messages across all sessions (limit 50)
SELECT
    m.session_id,
    m.message_id   AS id,
    m.auteur       AS author,
    m.contenu      AS content,
    m.horodatage   AS timestamp
FROM messages m
ORDER BY m.horodatage DESC
LIMIT 50;


DELETE FROM sessions WHERE session_id = :id;


TRUNCATE TABLE sessions RESTART IDENTITY CASCADE;


TRUNCATE TABLE messages RESTART IDENTITY;

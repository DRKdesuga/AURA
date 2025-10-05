CREATE TABLE IF NOT EXISTS sessions (
                                        session_id   BIGSERIAL PRIMARY KEY,
                                        date_debut   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                        date_fin     TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS messages (
                                        message_id   BIGSERIAL PRIMARY KEY,
                                        auteur       VARCHAR(16) NOT NULL CHECK (auteur IN ('USER','ASSISTANT')),
                                        contenu      TEXT NOT NULL,
                                        horodatage   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                        session_id   BIGINT NOT NULL REFERENCES sessions(session_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_messages_session_time
    ON messages (session_id, horodatage);

ALTER TABLE sessions ADD COLUMN IF NOT EXISTS title VARCHAR(200);
CREATE INDEX IF NOT EXISTS idx_sessions_title_lower ON sessions (LOWER(title));

WITH first_user AS (
    SELECT DISTINCT ON (m.session_id)
        m.session_id,
        m.contenu AS first_user_content
    FROM messages m
    WHERE m.auteur = 'USER'
    ORDER BY m.session_id, m.horodatage ASC
)
UPDATE sessions s
SET title = fu.first_user_content
FROM first_user fu
WHERE fu.session_id = s.session_id
  AND s.title IS NULL;



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

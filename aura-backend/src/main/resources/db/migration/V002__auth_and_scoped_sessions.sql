-- Create users, chat_sessions, and chat_messages tables with relationships
CREATE TABLE IF NOT EXISTS users (
    id            UUID PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    username      VARCHAR(100) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(16)  NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS chat_sessions (
    session_id BIGSERIAL PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date_debut TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    date_fin   TIMESTAMPTZ,
    title      VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS chat_messages (
    message_id  BIGSERIAL PRIMARY KEY,
    auteur      VARCHAR(16) NOT NULL CHECK (auteur IN ('USER','ASSISTANT')),
    contenu     TEXT NOT NULL,
    horodatage  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    session_id  BIGINT NOT NULL REFERENCES chat_sessions(session_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_messages_session_time
    ON chat_messages (session_id, horodatage);

CREATE INDEX IF NOT EXISTS idx_sessions_title_lower ON chat_sessions (LOWER(title));
CREATE INDEX IF NOT EXISTS idx_sessions_user ON chat_sessions (user_id);

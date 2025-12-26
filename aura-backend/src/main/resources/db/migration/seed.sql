-- Core tables
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

CREATE INDEX IF NOT EXISTS idx_messages_session_time ON chat_messages (session_id, horodatage);
CREATE INDEX IF NOT EXISTS idx_sessions_title_lower ON chat_sessions (LOWER(title));
CREATE INDEX IF NOT EXISTS idx_sessions_user ON chat_sessions (user_id);

-- Seed users (update password hashes with BCrypt before production)
INSERT INTO users (id, email, username, password_hash, role, enabled, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001', 'admin@aura.local', 'admin',
        '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5CRQx5a1S4I/V6ZUJqKfqx/1X8dS', 'ADMIN', TRUE, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, email, username, password_hash, role, enabled, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000002', 'user@aura.local', 'user',
        '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5CRQx5a1S4I/V6ZUJqKfqx/1X8dS', 'USER', TRUE, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Sample session/message for user@aura.local
INSERT INTO chat_sessions (session_id, user_id, date_debut, title)
VALUES (1, '00000000-0000-0000-0000-000000000002', NOW(), 'Sample chat')
ON CONFLICT (session_id) DO NOTHING;

INSERT INTO chat_messages (message_id, auteur, contenu, horodatage, session_id)
VALUES (1, 'USER', 'Hello from the sample seed!', NOW(), 1)
ON CONFLICT (message_id) DO NOTHING;

-- To update the admin password hash, generate a BCrypt hash (e.g., via Spring Security BCryptPasswordEncoder)
-- and replace the password_hash value above for admin@aura.local.

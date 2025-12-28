-- Patch existing schema to support users + per-user chat isolation without data loss.

-- 1) Ensure users table exists
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'users') THEN
        CREATE TABLE users (
            id            UUID PRIMARY KEY,
            email         VARCHAR(255) NOT NULL UNIQUE,
            username      VARCHAR(100) UNIQUE,
            password_hash VARCHAR(255) NOT NULL,
            role          VARCHAR(16)  NOT NULL,
            enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
            created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
            updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
        );
    END IF;
END $$;

-- 2) Rename legacy tables to new names if needed
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'sessions')
       AND NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'chat_sessions') THEN
        ALTER TABLE sessions RENAME TO chat_sessions;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'messages')
       AND NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'chat_messages') THEN
        ALTER TABLE messages RENAME TO chat_messages;
    END IF;
END $$;

-- Ensure core tables exist (for fresh environments where they might be absent)
CREATE TABLE IF NOT EXISTS chat_sessions (
    session_id BIGSERIAL PRIMARY KEY,
    user_id    UUID,
    date_debut TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    date_fin   TIMESTAMPTZ,
    title      VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS chat_messages (
    message_id  BIGSERIAL PRIMARY KEY,
    auteur      VARCHAR(16) NOT NULL CHECK (auteur IN ('USER','ASSISTANT')),
    contenu     TEXT NOT NULL,
    horodatage  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    session_id  BIGINT NOT NULL
);

-- 3) Add user_id to chat_sessions (nullable for backfill)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'chat_sessions' AND column_name = 'user_id'
    ) THEN
        ALTER TABLE chat_sessions ADD COLUMN user_id UUID;
    END IF;
END $$;

-- 4) Legacy user used to backfill existing sessions
INSERT INTO users (id, email, username, password_hash, role, enabled, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-0000000000aa', 'legacy@aura.local', 'legacy',
        '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5CRQx5a1S4I/V6ZUJqKfqx/1X8dS', 'USER', TRUE, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 5) Backfill user_id for existing sessions
UPDATE chat_sessions SET user_id = '00000000-0000-0000-0000-0000000000aa'
WHERE user_id IS NULL;

-- 6) Enforce NOT NULL and FK
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'chat_sessions' AND column_name = 'user_id' AND is_nullable = 'YES'
    ) THEN
        ALTER TABLE chat_sessions ALTER COLUMN user_id SET NOT NULL;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'chat_sessions' AND constraint_type = 'FOREIGN KEY' AND constraint_name = 'fk_chat_sessions_user'
    ) THEN
        ALTER TABLE chat_sessions
            ADD CONSTRAINT fk_chat_sessions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
    END IF;
END $$;

-- 7) Refresh indexes and constraints on chat_messages
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'chat_messages' AND constraint_type = 'FOREIGN KEY' AND constraint_name = 'fk_chat_messages_session'
    ) THEN
        ALTER TABLE chat_messages
            ADD CONSTRAINT fk_chat_messages_session FOREIGN KEY (session_id) REFERENCES chat_sessions(session_id) ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_chat_sessions_user ON chat_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_session_time ON chat_messages(session_id, horodatage);

-- 8) Make sure user uniqueness is enforced
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email_unique ON users(LOWER(email));
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username_unique ON users(LOWER(username));

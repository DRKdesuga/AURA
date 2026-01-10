-- Adds context persistence columns to chat_sessions.
ALTER TABLE chat_sessions ADD COLUMN IF NOT EXISTS memory_json TEXT;
ALTER TABLE chat_sessions ADD COLUMN IF NOT EXISTS summary_text TEXT;
ALTER TABLE chat_sessions ADD COLUMN IF NOT EXISTS last_memory_message_id BIGINT;

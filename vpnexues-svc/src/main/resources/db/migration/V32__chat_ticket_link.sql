-- V32: link mirrored chat tickets back to their session + allow guest rows.
-- Every chat message files a ticket row instantly (even before the visitor
-- identifies), so nothing is lost if they abandon the name/email prompt.
ALTER TABLE contact_messages ADD COLUMN IF NOT EXISTS chat_session_id UUID
    REFERENCES support_chat_sessions(id) ON DELETE CASCADE;
CREATE INDEX IF NOT EXISTS idx_contact_messages_chat_session ON contact_messages(chat_session_id);
ALTER TABLE contact_messages ALTER COLUMN email DROP NOT NULL;

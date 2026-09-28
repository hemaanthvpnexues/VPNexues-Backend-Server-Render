-- V31: live-chat sessions start as provisional guests (name/email captured
-- after the first message), so every chat is stored even if the visitor
-- abandons before identifying. Email stays optional until contact arrives.
ALTER TABLE support_chat_sessions ALTER COLUMN visitor_email DROP NOT NULL;

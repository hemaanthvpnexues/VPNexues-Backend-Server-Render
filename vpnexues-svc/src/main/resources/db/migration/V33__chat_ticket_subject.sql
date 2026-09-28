-- V33: chat ticket rows keep a fixed short subject ("Live chat") so the
-- inbox table stays clean; the visitor's message is shown only in the
-- detail drawer. Normalises rows mirrored before this rule existed.
UPDATE contact_messages SET subject = 'Live chat' WHERE chat_session_id IS NOT NULL;

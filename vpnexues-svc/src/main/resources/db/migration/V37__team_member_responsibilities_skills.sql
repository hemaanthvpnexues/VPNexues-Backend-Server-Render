-- Per-member "Roles & Responsibilities" (JSON array of {title, description}) and
-- "Key Skills" (JSON array of strings) for the public /team/:slug detail page.
-- Nullable: rows created before this migration keep showing the role-based
-- defaults until an admin saves an edit (which stores explicit values here).

ALTER TABLE team_members ADD COLUMN responsibilities TEXT;
ALTER TABLE team_members ADD COLUMN key_skills TEXT;

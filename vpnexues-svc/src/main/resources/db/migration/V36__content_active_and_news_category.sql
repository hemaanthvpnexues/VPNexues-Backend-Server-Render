-- Active/inactive flag for public content + a news category label.
-- Both tables stay populated from the V34 seed, so backfill = DEFAULT TRUE.

ALTER TABLE team_members ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE news_articles ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE news_articles ADD COLUMN category VARCHAR(100);

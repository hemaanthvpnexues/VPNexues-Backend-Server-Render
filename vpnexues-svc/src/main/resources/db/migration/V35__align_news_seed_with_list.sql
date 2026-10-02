-- Align the seeded news rows with the static /news LIST data (News.tsx) —
-- the list is the primary view, so its image/tags win over the detail-only
-- ARTICLES copy. V34 had taken its values from BlogDetail's ARTICLES, where
-- article 4 used a page-banner image and article 1 carried an extra Export tag.
-- Append-only: V34 is already applied and must not be edited (agents.md).

UPDATE news_articles
SET image_url = 'https://res.cloudinary.com/eqjshlur/image/upload/v1790169769/vpnexues/news/news-tractor.png'
WHERE legacy_key = '4';

UPDATE news_articles
SET tags = 'Organic,Farm,Fresh'
WHERE legacy_key = '1';

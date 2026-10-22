-- Delivery-city list correction (product decision, Oct 2026).
--
-- The three deliverable Indian cities are Salem, Chennai and Coimbatore.
-- Tirupattur was seeded by mistake in V51 (already applied everywhere), so it
-- is removed here and Chennai is added instead. Append-only rule: V51 is never
-- edited — this migration supersedes its seed.
--
--   IN after this migration: salem / chennai / coimbatore
--   SG: unchanged ('*' wildcard = anywhere in Singapore)
--   US / AE / LK / MY: unchanged (no rows -> "Coming Soon" everywhere)
DELETE FROM serviceable_cities
WHERE country_code = 'IN'
  AND city_name_normalized IN ('tirupattur', 'tirupathur');

INSERT INTO serviceable_cities (country_code, city_name, city_name_normalized) VALUES
    ('IN', 'Chennai', 'chennai')
ON CONFLICT DO NOTHING;

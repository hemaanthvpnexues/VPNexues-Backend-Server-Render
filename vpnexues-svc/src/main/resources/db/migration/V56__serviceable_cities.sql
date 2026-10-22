-- Delivery-city availability: the cities VPNexues actually delivers to.
-- Checked in OrderService.checkout BEFORE any order/payment row is written, so a
-- non-deliverable city can never be confirmed, charged or silently discarded —
-- the UI shows the "Coming Soon" page instead of "Order received".
-- Append-only migration; never edit once applied.
--
-- Seeded coverage (product decision, Oct 2026):
--   SG: '*' wildcard row = delivery available everywhere in Singapore.
--   IN: Salem, Tirupattur, Coimbatore only — every other Indian city is
--       "Coming Soon". Both accepted spellings of Tirupattur are seeded so a
--       geocoder variant ("Tirupathur") cannot lock real customers out.
--   US / AE / LK / MY: no rows — every city falls through to "Coming Soon".
-- Matching is case- and whitespace-insensitive (see DeliveryAvailabilityService).
CREATE TABLE IF NOT EXISTS serviceable_cities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    country_code VARCHAR(4) NOT NULL,
    city_name VARCHAR(120) NOT NULL,
    city_name_normalized VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_serviceable_cities_country_city
    ON serviceable_cities (country_code, city_name_normalized);

CREATE INDEX IF NOT EXISTS idx_serviceable_cities_country_active
    ON serviceable_cities (country_code, active);

INSERT INTO serviceable_cities (country_code, city_name, city_name_normalized) VALUES
    ('SG', '*', '*'),
    ('IN', 'Salem', 'salem'),
    ('IN', 'Tirupattur', 'tirupattur'),
    ('IN', 'Tirupathur', 'tirupathur'),
    ('IN', 'Coimbatore', 'coimbatore')
ON CONFLICT DO NOTHING;

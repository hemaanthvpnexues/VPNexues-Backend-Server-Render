-- Phase 3 admin schema. Additive-only — see V1 comment, same rule applies here.

CREATE TABLE categories (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL UNIQUE,
    slug            VARCHAR(100) NOT NULL UNIQUE,
    emoji           VARCHAR(10),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- password_hash holds a BCrypt hash only, produced via the app's PasswordEncoder bean —
-- never insert plaintext here. Bootstrap super-admin is created at app startup from env
-- vars (see AdminBootstrapRunner in Phase 3 backend), not seeded via SQL.
CREATE TABLE admin_users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    role            VARCHAR(20) NOT NULL DEFAULT 'ADMIN',
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE inventory_items (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id          UUID NOT NULL UNIQUE REFERENCES products(id) ON DELETE CASCADE,
    quantity            INTEGER NOT NULL DEFAULT 0,
    reorder_threshold   INTEGER NOT NULL DEFAULT 10,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE b2b_enquiries (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_name        VARCHAR(255) NOT NULL,
    contact_name        VARCHAR(255) NOT NULL,
    email               VARCHAR(255) NOT NULL,
    phone               VARCHAR(20),
    country_code        VARCHAR(4) NOT NULL,
    product_interest    VARCHAR(255),
    quantity            VARCHAR(100),
    estimated_value     NUMERIC(12,2),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_b2b_enquiries_status ON b2b_enquiries(status);

CREATE TABLE testimonials (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    role        VARCHAR(255),
    location    VARCHAR(255),
    rating      SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    quote       TEXT NOT NULL,
    photo_url   VARCHAR(500),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE contact_messages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    phone           VARCHAR(20),
    country_code    VARCHAR(4),
    subject         VARCHAR(255) NOT NULL,
    message         TEXT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'NEW',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_contact_messages_status ON contact_messages(status);

-- Single-row settings table. General/Store/Shipping/Social columns only — no SMTP
-- credentials or payment-gateway secrets live here (those tabs are UI-only in the
-- admin frontend until a real integration is wired up later).
CREATE TABLE store_settings (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    website_name                VARCHAR(255),
    tagline                     VARCHAR(255),
    default_currency            VARCHAR(4) NOT NULL DEFAULT 'SGD',
    timezone                    VARCHAR(100),
    date_format                 VARCHAR(20),
    time_format                 VARCHAR(20),
    website_email               VARCHAR(255),
    website_phone               VARCHAR(20),
    store_name                  VARCHAR(255),
    store_email                 VARCHAR(255),
    store_phone                 VARCHAR(20),
    store_address               TEXT,
    store_description           TEXT,
    free_shipping_threshold     NUMERIC(12,2),
    standard_shipping_rate      NUMERIC(12,2),
    express_shipping_rate       NUMERIC(12,2),
    default_delivery_days       INTEGER,
    facebook_url                VARCHAR(500),
    instagram_url               VARCHAR(500),
    twitter_url                 VARCHAR(500),
    youtube_url                 VARCHAR(500),
    whatsapp_url                VARCHAR(500),
    telegram_url                VARCHAR(500),
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now()
);

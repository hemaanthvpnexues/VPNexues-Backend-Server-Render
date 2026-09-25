-- Phase 1 core commerce schema. Additive-only going forward — never edit this
-- file after it has been applied; add a new V{n}__ migration instead.

CREATE TABLE products (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(255) NOT NULL UNIQUE,
    category        VARCHAR(100) NOT NULL,
    description     TEXT,
    base_price      NUMERIC(12,2) NOT NULL,
    old_price       NUMERIC(12,2),
    unit            VARCHAR(50) NOT NULL,
    sku             VARCHAR(100) NOT NULL UNIQUE,
    image_url       VARCHAR(500),
    discount_pct    INTEGER,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE product_badges (
    product_id      UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    badge           VARCHAR(50) NOT NULL
);
CREATE INDEX idx_product_badges_product_id ON product_badges(product_id);

CREATE TABLE product_price_overrides (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id      UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    country_code    VARCHAR(4) NOT NULL,
    price           NUMERIC(12,2) NOT NULL,
    old_price       NUMERIC(12,2),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_price_override_product_country UNIQUE (product_id, country_code)
);

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone           VARCHAR(20) NOT NULL UNIQUE,
    name            VARCHAR(255),
    email           VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE user_addresses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    label           VARCHAR(50) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    phone           VARCHAR(20) NOT NULL,
    address_line    TEXT NOT NULL,
    flat            VARCHAR(255),
    landmark        VARCHAR(255),
    city            VARCHAR(100) NOT NULL,
    pincode         VARCHAR(20) NOT NULL,
    state           VARCHAR(100) NOT NULL,
    is_default      BOOLEAN NOT NULL DEFAULT FALSE,
    lat             DOUBLE PRECISION,
    lng             DOUBLE PRECISION,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_user_addresses_user_id ON user_addresses(user_id);

CREATE TABLE carts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    guest_token     VARCHAR(255) UNIQUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_cart_owner CHECK (user_id IS NOT NULL OR guest_token IS NOT NULL)
);

CREATE TABLE cart_items (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id                 UUID NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    product_id              UUID REFERENCES products(id),
    box_type                VARCHAR(20),
    qty                     INTEGER NOT NULL CHECK (qty > 0),
    unit_price_snapshot     NUMERIC(12,2) NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_cart_item_kind CHECK (product_id IS NOT NULL OR box_type IS NOT NULL)
);
CREATE INDEX idx_cart_items_cart_id ON cart_items(cart_id);

CREATE TABLE orders (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number    VARCHAR(30) NOT NULL UNIQUE,
    user_id         UUID NOT NULL REFERENCES users(id),
    address_id      UUID NOT NULL REFERENCES user_addresses(id),
    status          VARCHAR(20) NOT NULL DEFAULT 'PLACED',
    channel         VARCHAR(20) NOT NULL DEFAULT 'SHOP',
    item_total      NUMERIC(12,2) NOT NULL,
    discount        NUMERIC(12,2) NOT NULL DEFAULT 0,
    delivery_fee    NUMERIC(12,2) NOT NULL DEFAULT 0,
    tax             NUMERIC(12,2) NOT NULL DEFAULT 0,
    tip             NUMERIC(12,2) NOT NULL DEFAULT 0,
    grand_total     NUMERIC(12,2) NOT NULL,
    payment_method  VARCHAR(50) NOT NULL,
    coupon_code     VARCHAR(50),
    country_code    VARCHAR(4) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);

CREATE TABLE order_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id      UUID REFERENCES products(id),
    box_type        VARCHAR(20),
    name            VARCHAR(255) NOT NULL,
    image_url       VARCHAR(500),
    qty             INTEGER NOT NULL CHECK (qty > 0),
    unit_price      NUMERIC(12,2) NOT NULL,
    line_total      NUMERIC(12,2) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);

CREATE TABLE payments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID NOT NULL UNIQUE REFERENCES orders(id),
    amount          NUMERIC(12,2) NOT NULL,
    method          VARCHAR(50) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    paid_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE coupons (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(50) NOT NULL UNIQUE,
    type            VARCHAR(10) NOT NULL,
    value           NUMERIC(12,2) NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    min_order_value NUMERIC(12,2),
    expires_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

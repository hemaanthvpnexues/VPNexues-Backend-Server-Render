-- Career applications: store candidate details submitted via /careers/apply.
-- Append-only migration; never edit once applied.
CREATE TABLE IF NOT EXISTS career_applications (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    location VARCHAR(255),
    role VARCHAR(255) NOT NULL,
    linkedin VARCHAR(500),
    github VARCHAR(500),
    resume VARCHAR(1000),
    availability VARCHAR(100),
    source VARCHAR(100),
    why_join TEXT,
    timestamp VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

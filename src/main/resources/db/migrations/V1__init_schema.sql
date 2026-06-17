-- 1. Create Users Table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    google_sub VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    refresh_token TEXT,
    token_expiry TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 2. Create Skills Table
CREATE TABLE skills (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(150) NOT NULL,
    sentence_fr TEXT NOT NULL,
    sentence_en TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 3. Create Templates Table
CREATE TABLE templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    subject_template TEXT NOT NULL,
    body_template TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 4. Create CV Variants Table
CREATE TABLE cv_variants (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    language VARCHAR(10) NOT NULL,
    file_url TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 5. Create Applications Tracking Table
CREATE TABLE applications (
    id BIGSERIAL PRIMARY KEY,
    company_name VARCHAR(150) NOT NULL,
    job_title VARCHAR(150) NOT NULL,
    recipient_email VARCHAR(255),
    language VARCHAR(10) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'Sent',
    generated_subject TEXT NOT NULL,
    generated_body TEXT NOT NULL,
    date_applied TIMESTAMP NOT NULL DEFAULT NOW(),
    notes TEXT,
    template_id BIGINT REFERENCES templates(id) ON DELETE SET NULL,
    cv_variant_id BIGINT REFERENCES cv_variants(id) ON DELETE SET NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE
);

-- 6. Create Many-to-Many Join Table (Applications <-> Skills)
CREATE TABLE application_skills (
    application_id BIGINT NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    skill_id BIGINT NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    PRIMARY KEY (application_id, skill_id)
);
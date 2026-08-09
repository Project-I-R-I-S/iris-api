-- =========================================================================
-- I.R.I.S — V1 initial schema
-- Covers: users, auth, nutrition (food + drinks), hydration, weight, sleep,
-- caffeine (as a field on nutrition), reminders/notifications.
-- =========================================================================

-- ---------- Extensions ----------
CREATE EXTENSION IF NOT EXISTS "pgcrypto";  -- for gen_random_uuid()

-- ---------- Users ----------
CREATE TABLE users (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email             VARCHAR(255) NOT NULL UNIQUE,
    password_hash     VARCHAR(255),                    -- null for Google-only accounts
    google_subject    VARCHAR(255) UNIQUE,             -- Google's `sub` claim, null for email-only
    display_name      VARCHAR(120),
    date_of_birth     DATE,
    sex               VARCHAR(16),                     -- 'male', 'female', 'other', 'prefer_not_to_say'
    height_cm         NUMERIC(5, 2),
    day_start_time    TIME         NOT NULL DEFAULT '07:00',
    day_end_time      TIME         NOT NULL DEFAULT '23:00',
    timezone          VARCHAR(64)  NOT NULL DEFAULT 'Asia/Kolkata',
    daily_water_goal_ml     INTEGER NOT NULL DEFAULT 2500,
    daily_calorie_goal      INTEGER,
    daily_caffeine_limit_mg INTEGER NOT NULL DEFAULT 400,
    email_verified    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_email ON users (email);

-- ---------- Refresh tokens ----------
CREATE TABLE refresh_tokens (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash    VARCHAR(255) NOT NULL UNIQUE,        -- store hash, never the raw token
    expires_at    TIMESTAMPTZ  NOT NULL,
    revoked_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    user_agent    VARCHAR(255),
    ip_address    VARCHAR(64)
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);

-- ---------- Nutrition entries ----------
-- Covers BOTH solid foods and drinks (juice, coffee, etc).
-- Water is a separate, simpler table for quick logging.
CREATE TABLE food_entries (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name              VARCHAR(255) NOT NULL,
    brand             VARCHAR(120),
    meal_type         VARCHAR(32)  NOT NULL,           -- 'breakfast','lunch','dinner','snack','drink'
    serving_size      NUMERIC(10, 2) NOT NULL,
    serving_unit      VARCHAR(32)  NOT NULL,           -- 'g','ml','piece','cup', etc.
    calories          NUMERIC(10, 2) NOT NULL,
    protein_g         NUMERIC(10, 2) NOT NULL DEFAULT 0,
    carbs_g           NUMERIC(10, 2) NOT NULL DEFAULT 0,
    fat_g             NUMERIC(10, 2) NOT NULL DEFAULT 0,
    fiber_g           NUMERIC(10, 2) NOT NULL DEFAULT 0,
    sugar_g           NUMERIC(10, 2),
    sodium_mg         NUMERIC(10, 2),
    caffeine_mg       NUMERIC(10, 2) NOT NULL DEFAULT 0,
    fluid_ml          NUMERIC(10, 2),                  -- populated for drinks; counts toward hydration
    notes             VARCHAR(500),
    consumed_at       TIMESTAMPTZ  NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_food_entries_user_consumed ON food_entries (user_id, consumed_at DESC);

-- ---------- Water entries ----------
CREATE TABLE water_entries (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount_ml     INTEGER      NOT NULL CHECK (amount_ml > 0),
    consumed_at   TIMESTAMPTZ  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_water_entries_user_consumed ON water_entries (user_id, consumed_at DESC);

-- ---------- Weight entries ----------
CREATE TABLE weight_entries (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    weight_kg     NUMERIC(5, 2) NOT NULL CHECK (weight_kg > 0),
    recorded_at   TIMESTAMPTZ  NOT NULL,
    notes         VARCHAR(500),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_weight_entries_user_recorded ON weight_entries (user_id, recorded_at DESC);

-- ---------- Sleep entries ----------
CREATE TABLE sleep_entries (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    slept_at      TIMESTAMPTZ  NOT NULL,
    woke_at       TIMESTAMPTZ  NOT NULL,
    quality       SMALLINT     CHECK (quality BETWEEN 1 AND 5),
    notes         VARCHAR(500),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CHECK (woke_at > slept_at)
);

CREATE INDEX idx_sleep_entries_user_slept ON sleep_entries (user_id, slept_at DESC);

-- ---------- Reminders / notifications ----------
CREATE TABLE reminders (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reminder_type   VARCHAR(32)  NOT NULL,     -- 'water','sleep','end_of_day_summary','custom'
    time_of_day     TIME,                       -- for daily-at-hh:mm reminders
    interval_minutes INTEGER,                   -- for interval reminders (e.g. water every 90m)
    days_of_week    SMALLINT NOT NULL DEFAULT 127,  -- bitmask, Sun..Sat = bit 0..6, 127 = every day
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    label           VARCHAR(120),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_reminders_user_enabled ON reminders (user_id, enabled);

-- ---------- Push device tokens ----------
-- Stores Expo / FCM / APNs tokens so the reminder scheduler can push notifications.
CREATE TABLE device_tokens (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token         VARCHAR(500) NOT NULL UNIQUE,
    platform      VARCHAR(16)  NOT NULL,       -- 'ios','android','web'
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_used_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_device_tokens_user_id ON device_tokens (user_id);

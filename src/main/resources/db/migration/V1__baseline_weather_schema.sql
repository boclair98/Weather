CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    coders_user_id VARCHAR(64) UNIQUE,
    owner_id VARCHAR(64),
    subscribed BOOLEAN NOT NULL DEFAULT TRUE,
    unsubscribe_token VARCHAR(64) UNIQUE,
    location_name VARCHAR(255) NOT NULL DEFAULT '서울특별시 중구',
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    nx INTEGER NOT NULL DEFAULT 60,
    ny INTEGER NOT NULL DEFAULT 127,
    age_group VARCHAR(32),
    gender VARCHAR(32),
    temperature_sensitivity VARCHAR(32),
    activity_type VARCHAR(32),
    smart_alert_enabled BOOLEAN,
    rain_alert_enabled BOOLEAN,
    temperature_alert_enabled BOOLEAN,
    air_quality_alert_enabled BOOLEAN,
    wind_alert_enabled BOOLEAN,
    last_smart_alert_fingerprint VARCHAR(160),
    last_smart_alert_at TIMESTAMP,
    morning_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    afternoon_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    evening_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP,
    privacy_consent_version VARCHAR(32),
    privacy_consent_at TIMESTAMP,
    unsubscribed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_subscribed ON users(subscribed);
CREATE INDEX IF NOT EXISTS idx_users_coders_user ON users(coders_user_id);
CREATE INDEX IF NOT EXISTS idx_users_owner ON users(owner_id);

CREATE TABLE IF NOT EXISTS weather_mail_histories (
    id BIGSERIAL PRIMARY KEY,
    user_email VARCHAR(255) NOT NULL,
    location_name VARCHAR(255) NOT NULL,
    period VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    forecast_date VARCHAR(16),
    forecast_time VARCHAR(16),
    error_message VARCHAR(1000),
    sent_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_mail_history_sent_at
    ON weather_mail_histories(sent_at);
CREATE INDEX IF NOT EXISTS idx_mail_history_email_sent_at
    ON weather_mail_histories(user_email, sent_at);

CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL
);

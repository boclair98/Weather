ALTER TABLE users
    ADD COLUMN IF NOT EXISTS morning_time TIME NOT NULL DEFAULT '06:30:00';

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS afternoon_time TIME NOT NULL DEFAULT '11:30:00';

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS evening_time TIME NOT NULL DEFAULT '18:30:00';

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS last_morning_mail_date DATE;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS last_afternoon_mail_date DATE;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS last_evening_mail_date DATE;

CREATE INDEX IF NOT EXISTS idx_users_morning_schedule
    ON users(subscribed, morning_enabled, morning_time);

CREATE INDEX IF NOT EXISTS idx_users_afternoon_schedule
    ON users(subscribed, afternoon_enabled, afternoon_time);

CREATE INDEX IF NOT EXISTS idx_users_evening_schedule
    ON users(subscribed, evening_enabled, evening_time);

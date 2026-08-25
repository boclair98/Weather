-- One-time privacy reset requested before removing outbound email features.
-- Keep the schema and Flyway history intact; delete only application rows.
DELETE FROM weather_mail_histories;
DELETE FROM users;
DELETE FROM shedlock;

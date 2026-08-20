package com.example.WebSideProject.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseResetMigrationTest {

    @Test
    void resetMigrationDeletesOnlyLegacyApplicationRows() throws IOException {
        String migration;
        try (InputStream stream = getClass().getResourceAsStream(
                "/db/migration/V3__remove_email_subscription_data.sql")) {
            if (stream == null) {
                throw new IOException("Reset migration not found");
            }
            migration = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertThat(migration)
                .contains("DELETE FROM weather_mail_histories")
                .contains("DELETE FROM users")
                .contains("DELETE FROM shedlock")
                .doesNotContain("DROP TABLE")
                .doesNotContain("TRUNCATE DATABASE");
    }
}

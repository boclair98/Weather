package com.example.WebSideProject.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserIdentityTest {

    @Test
    void legacySubscriptionCanClaimCodersIdentityOnce() {
        User user = user(null);

        user.claimCodersIdentity("coders-user-1");
        user.claimCodersIdentity("coders-user-2");

        assertThat(user.getOwnerId()).isEqualTo("coders-user-1");
    }

    @Test
    void existingCodersIdentityIsNeverOverwritten() {
        User user = user("owner-id");

        user.claimCodersIdentity("attacker-id");

        assertThat(user.getCodersUserId()).isEqualTo("owner-id");
        assertThat(user.getOwnerId()).isNull();
        assertThat(user.isOwnedBy("owner-id")).isTrue();
    }

    @Test
    void notificationTimesAlwaysKeepAtLeastOneDelivery() {
        User user = user("owner-id");

        user.updateNotificationTimes(false, false, false);

        assertThat(user.isMorningEnabled()).isTrue();
        assertThat(user.isAfternoonEnabled()).isFalse();
        assertThat(user.isEveningEnabled()).isFalse();
    }

    private User user(String codersUserId) {
        return User.builder()
                .name("테스트")
                .email("test@example.com")
                .codersUserId(codersUserId)
                .locationName("서울")
                .nx(60)
                .ny(127)
                .morningEnabled(true)
                .build();
    }
}

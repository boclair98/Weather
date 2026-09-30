package com.example.WebSideProject.dto;

import com.example.WebSideProject.Enum.MailSendStatus;

import java.util.List;

/** Only the authenticated subscriber's own delivery information is returned. */
public record DeliveryOverviewDto(
        boolean subscribed,
        String nextScheduledAt,
        String timezone,
        List<DeliveryAttempt> recentAttempts
) {
    public record DeliveryAttempt(
            String sentAt,
            MailSendStatus status,
            String locationName
    ) {
    }
}

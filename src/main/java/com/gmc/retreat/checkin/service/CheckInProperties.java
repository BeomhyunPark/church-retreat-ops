package com.gmc.retreat.checkin.service;

import java.time.OffsetDateTime;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.check-in")
public record CheckInProperties(
        OffsetDateTime qrExpiresAt
) {
}

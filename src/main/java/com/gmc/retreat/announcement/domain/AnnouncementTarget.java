package com.gmc.retreat.announcement.domain;

import java.time.OffsetDateTime;

public record AnnouncementTarget(
        Long id,
        Long announcementId,
        AnnouncementTargetType targetType,
        String targetValue,
        OffsetDateTime createdAt
) {
}

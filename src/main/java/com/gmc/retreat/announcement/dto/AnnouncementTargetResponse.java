package com.gmc.retreat.announcement.dto;

import com.gmc.retreat.announcement.domain.AnnouncementTarget;
import com.gmc.retreat.announcement.domain.AnnouncementTargetType;
import java.time.OffsetDateTime;

public record AnnouncementTargetResponse(
        Long id,
        AnnouncementTargetType targetType,
        String targetValue,
        OffsetDateTime createdAt
) {
    public static AnnouncementTargetResponse from(AnnouncementTarget target) {
        return new AnnouncementTargetResponse(
                target.id(),
                target.targetType(),
                target.targetValue(),
                target.createdAt()
        );
    }
}

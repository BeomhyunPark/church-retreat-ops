package com.gmc.retreat.announcement.mapper;

import com.gmc.retreat.announcement.domain.AnnouncementTargetType;

public record AnnouncementTargetInsert(
        Long announcementId,
        AnnouncementTargetType targetType,
        String targetValue
) {
}

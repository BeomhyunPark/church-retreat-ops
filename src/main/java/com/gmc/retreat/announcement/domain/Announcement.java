package com.gmc.retreat.announcement.domain;

import com.gmc.retreat.admin.domain.AdminRole;
import java.time.OffsetDateTime;

public record Announcement(
        Long id,
        String title,
        String content,
        Boolean pinned,
        Boolean active,
        OffsetDateTime visibleFrom,
        OffsetDateTime visibleUntil,
        Long createdByAdminId,
        String createdByAdminEmail,
        String createdByAdminName,
        AdminRole createdByAdminRole,
        Long updatedByAdminId,
        String updatedByAdminEmail,
        String updatedByAdminName,
        AdminRole updatedByAdminRole,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

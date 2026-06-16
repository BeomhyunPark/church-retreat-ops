package com.gmc.retreat.announcement.dto;

import com.gmc.retreat.admin.dto.AdminSummaryResponse;
import com.gmc.retreat.announcement.domain.Announcement;
import com.gmc.retreat.announcement.domain.AnnouncementTarget;
import java.time.OffsetDateTime;
import java.util.List;

public record AnnouncementResponse(
        Long id,
        String title,
        String content,
        Boolean pinned,
        Boolean active,
        OffsetDateTime visibleFrom,
        OffsetDateTime visibleUntil,
        List<AnnouncementTargetResponse> targets,
        AdminSummaryResponse createdBy,
        AdminSummaryResponse updatedBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static AnnouncementResponse from(Announcement announcement, List<AnnouncementTarget> targets) {
        return new AnnouncementResponse(
                announcement.id(),
                announcement.title(),
                announcement.content(),
                announcement.pinned(),
                announcement.active(),
                announcement.visibleFrom(),
                announcement.visibleUntil(),
                targets.stream()
                        .map(AnnouncementTargetResponse::from)
                        .toList(),
                new AdminSummaryResponse(
                        announcement.createdByAdminId(),
                        announcement.createdByAdminEmail(),
                        announcement.createdByAdminName(),
                        announcement.createdByAdminRole()
                ),
                new AdminSummaryResponse(
                        announcement.updatedByAdminId(),
                        announcement.updatedByAdminEmail(),
                        announcement.updatedByAdminName(),
                        announcement.updatedByAdminRole()
                ),
                announcement.createdAt(),
                announcement.updatedAt()
        );
    }
}

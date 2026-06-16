package com.gmc.retreat.schedule.domain;

import com.gmc.retreat.admin.domain.AdminRole;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ScheduleItem(
        Long id,
        String title,
        String description,
        LocalDate scheduleDate,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        String location,
        ScheduleCategory category,
        ScheduleTargetAudience targetAudience,
        Boolean active,
        Integer displayOrder,
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

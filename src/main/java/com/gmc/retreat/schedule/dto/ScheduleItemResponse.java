package com.gmc.retreat.schedule.dto;

import com.gmc.retreat.admin.dto.AdminSummaryResponse;
import com.gmc.retreat.schedule.domain.ScheduleCategory;
import com.gmc.retreat.schedule.domain.ScheduleItem;
import com.gmc.retreat.schedule.domain.ScheduleTargetAudience;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ScheduleItemResponse(
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
        AdminSummaryResponse createdBy,
        AdminSummaryResponse updatedBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ScheduleItemResponse from(ScheduleItem scheduleItem) {
        return new ScheduleItemResponse(
                scheduleItem.id(),
                scheduleItem.title(),
                scheduleItem.description(),
                scheduleItem.scheduleDate(),
                scheduleItem.startsAt(),
                scheduleItem.endsAt(),
                scheduleItem.location(),
                scheduleItem.category(),
                scheduleItem.targetAudience(),
                scheduleItem.active(),
                scheduleItem.displayOrder(),
                new AdminSummaryResponse(
                        scheduleItem.createdByAdminId(),
                        scheduleItem.createdByAdminEmail(),
                        scheduleItem.createdByAdminName(),
                        scheduleItem.createdByAdminRole()
                ),
                new AdminSummaryResponse(
                        scheduleItem.updatedByAdminId(),
                        scheduleItem.updatedByAdminEmail(),
                        scheduleItem.updatedByAdminName(),
                        scheduleItem.updatedByAdminRole()
                ),
                scheduleItem.createdAt(),
                scheduleItem.updatedAt()
        );
    }
}

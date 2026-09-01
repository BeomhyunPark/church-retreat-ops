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
        Boolean collectParticipation,
        Long participationOptionId,
        Long selectionCount,
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
                scheduleItem.collectParticipation(),
                scheduleItem.participationOptionId(),
                scheduleItem.selectionCount(),
                actor(scheduleItem.createdByAdminId(), scheduleItem.createdByAdminEmail(),
                        scheduleItem.createdByAdminName(), scheduleItem.createdByAdminRole()),
                actor(scheduleItem.updatedByAdminId(), scheduleItem.updatedByAdminEmail(),
                        scheduleItem.updatedByAdminName(), scheduleItem.updatedByAdminRole()),
                scheduleItem.createdAt(),
                scheduleItem.updatedAt()
        );
    }

    private static AdminSummaryResponse actor(
            Long id,
            String email,
            String name,
            com.gmc.retreat.admin.domain.AdminRole role
    ) {
        return id == null ? null : new AdminSummaryResponse(id, email, name, role);
    }
}

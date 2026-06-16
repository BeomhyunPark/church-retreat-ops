package com.gmc.retreat.schedule.dto;

import com.gmc.retreat.schedule.domain.ScheduleCategory;
import com.gmc.retreat.schedule.domain.ScheduleTargetAudience;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ScheduleItemRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 10000) String description,
        @NotNull LocalDate scheduleDate,
        @NotNull OffsetDateTime startsAt,
        @NotNull OffsetDateTime endsAt,
        @Size(max = 150) String location,
        @NotNull ScheduleCategory category,
        @NotNull ScheduleTargetAudience targetAudience,
        @NotNull Boolean active,
        @NotNull @Min(0) Integer displayOrder
) {
}

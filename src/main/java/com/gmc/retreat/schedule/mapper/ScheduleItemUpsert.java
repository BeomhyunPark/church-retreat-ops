package com.gmc.retreat.schedule.mapper;

import com.gmc.retreat.schedule.domain.ScheduleCategory;
import com.gmc.retreat.schedule.domain.ScheduleTargetAudience;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public class ScheduleItemUpsert {
    private Long id;
    private final String title;
    private final String description;
    private final LocalDate scheduleDate;
    private final OffsetDateTime startsAt;
    private final OffsetDateTime endsAt;
    private final String location;
    private final ScheduleCategory category;
    private final ScheduleTargetAudience targetAudience;
    private final Boolean active;
    private final Integer displayOrder;
    private final Boolean collectParticipation;
    private final Long adminId;

    public ScheduleItemUpsert(
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
            Long adminId
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.scheduleDate = scheduleDate;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.location = location;
        this.category = category;
        this.targetAudience = targetAudience;
        this.active = active;
        this.displayOrder = displayOrder;
        this.collectParticipation = collectParticipation;
        this.adminId = adminId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getScheduleDate() {
        return scheduleDate;
    }

    public OffsetDateTime getStartsAt() {
        return startsAt;
    }

    public OffsetDateTime getEndsAt() {
        return endsAt;
    }

    public String getLocation() {
        return location;
    }

    public ScheduleCategory getCategory() {
        return category;
    }

    public ScheduleTargetAudience getTargetAudience() {
        return targetAudience;
    }

    public Boolean getActive() {
        return active;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public Boolean getCollectParticipation() {
        return collectParticipation;
    }

    public Long getAdminId() {
        return adminId;
    }
}

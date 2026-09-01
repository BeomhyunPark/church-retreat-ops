package com.gmc.retreat.participation.mapper;

import com.gmc.retreat.participation.domain.ParticipationOptionType;
import java.time.LocalDate;

public class ParticipationOptionUpsert {

    private Long id;
    private final Long scheduleItemId;
    private final ParticipationOptionType optionType;
    private final String label;
    private final LocalDate eventDate;
    private final Integer displayOrder;
    private final Boolean active;

    public ParticipationOptionUpsert(
            Long id,
            Long scheduleItemId,
            ParticipationOptionType optionType,
            String label,
            LocalDate eventDate,
            Integer displayOrder,
            Boolean active
    ) {
        this.id = id;
        this.scheduleItemId = scheduleItemId;
        this.optionType = optionType;
        this.label = label;
        this.eventDate = eventDate;
        this.displayOrder = displayOrder;
        this.active = active;
    }

    public Long getId() { return id; }
    public Long getScheduleItemId() { return scheduleItemId; }
    public ParticipationOptionType getOptionType() { return optionType; }
    public String getLabel() { return label; }
    public LocalDate getEventDate() { return eventDate; }
    public Integer getDisplayOrder() { return displayOrder; }
    public Boolean getActive() { return active; }
}

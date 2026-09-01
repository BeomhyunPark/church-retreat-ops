package com.gmc.retreat.participation.dto;

import com.gmc.retreat.participation.domain.ParticipationOption;
import com.gmc.retreat.participation.domain.ParticipationOptionType;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ParticipationOptionResponse(
        Long id,
        Long scheduleItemId,
        ParticipationOptionType optionType,
        String label,
        LocalDate eventDate,
        Integer displayOrder,
        Boolean active,
        Long selectionCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ParticipationOptionResponse from(ParticipationOption option) {
        return new ParticipationOptionResponse(
                option.id(), option.scheduleItemId(), option.optionType(), option.label(), option.eventDate(),
                option.displayOrder(), option.active(), option.selectionCount(),
                option.createdAt(), option.updatedAt()
        );
    }

}

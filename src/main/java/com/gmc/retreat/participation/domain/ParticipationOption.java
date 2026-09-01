package com.gmc.retreat.participation.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ParticipationOption(
        Long id,
        Long retreatId,
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
}

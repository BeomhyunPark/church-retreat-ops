package com.gmc.retreat.retreat.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record Retreat(
        Long id,
        String name,
        LocalDate startsOn,
        LocalDate endsOn,
        RetreatStatus status,
        Integer participantCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

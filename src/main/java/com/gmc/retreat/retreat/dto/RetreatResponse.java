package com.gmc.retreat.retreat.dto;

import com.gmc.retreat.retreat.domain.Retreat;
import com.gmc.retreat.retreat.domain.RetreatStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record RetreatResponse(
        Long id,
        String name,
        LocalDate startsOn,
        LocalDate endsOn,
        RetreatStatus status,
        Integer participantCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static RetreatResponse from(Retreat retreat) {
        return new RetreatResponse(
                retreat.id(),
                retreat.name(),
                retreat.startsOn(),
                retreat.endsOn(),
                retreat.status(),
                retreat.participantCount(),
                retreat.createdAt(),
                retreat.updatedAt()
        );
    }
}

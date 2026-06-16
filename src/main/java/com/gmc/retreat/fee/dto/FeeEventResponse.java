package com.gmc.retreat.fee.dto;

import com.gmc.retreat.fee.domain.FeeEvent;
import java.time.OffsetDateTime;

public record FeeEventResponse(
        Long id,
        Long participantId,
        Boolean previousFeePaid,
        Boolean newFeePaid,
        FeeAdminActorResponse changedBy,
        String reason,
        OffsetDateTime createdAt
) {
    public static FeeEventResponse from(FeeEvent event) {
        return new FeeEventResponse(
                event.id(),
                event.participantId(),
                event.previousFeePaid(),
                event.newFeePaid(),
                FeeRosterResponse.actor(event.changedByAdminId(), event.changedByAdminName()),
                event.reason(),
                event.createdAt()
        );
    }
}

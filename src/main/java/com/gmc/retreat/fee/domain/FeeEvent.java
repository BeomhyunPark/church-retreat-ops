package com.gmc.retreat.fee.domain;

import java.time.OffsetDateTime;

public record FeeEvent(
        Long id,
        Long participantId,
        Boolean previousFeePaid,
        Boolean newFeePaid,
        Long changedByAdminId,
        String changedByAdminName,
        String reason,
        OffsetDateTime createdAt
) {
}

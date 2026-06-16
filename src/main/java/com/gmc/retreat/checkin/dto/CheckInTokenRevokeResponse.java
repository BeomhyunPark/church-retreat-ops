package com.gmc.retreat.checkin.dto;

import java.time.OffsetDateTime;

public record CheckInTokenRevokeResponse(
        Long participantId,
        OffsetDateTime revokedAt
) {
}

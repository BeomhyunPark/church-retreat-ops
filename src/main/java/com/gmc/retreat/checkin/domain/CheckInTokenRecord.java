package com.gmc.retreat.checkin.domain;

import java.time.OffsetDateTime;

public record CheckInTokenRecord(
        Long participantId,
        OffsetDateTime expiresAt,
        OffsetDateTime revokedAt
) {
}

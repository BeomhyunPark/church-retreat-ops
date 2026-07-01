package com.gmc.retreat.checkin.dto;

import java.time.OffsetDateTime;

public record CheckInQrCredentialResponse(
        String token,
        OffsetDateTime expiresAt
) {
}

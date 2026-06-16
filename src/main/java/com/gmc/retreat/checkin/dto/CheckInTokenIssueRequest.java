package com.gmc.retreat.checkin.dto;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record CheckInTokenIssueRequest(
        @NotNull OffsetDateTime expiresAt
) {
}

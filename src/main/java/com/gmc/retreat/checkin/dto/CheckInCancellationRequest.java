package com.gmc.retreat.checkin.dto;

import jakarta.validation.constraints.Size;

public record CheckInCancellationRequest(
        @Size(max = 500) String reason
) {
}

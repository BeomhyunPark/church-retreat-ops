package com.gmc.retreat.checkin.dto;

import jakarta.validation.constraints.NotBlank;

public record CheckInQrScanRequest(
        @NotBlank String token
) {
}

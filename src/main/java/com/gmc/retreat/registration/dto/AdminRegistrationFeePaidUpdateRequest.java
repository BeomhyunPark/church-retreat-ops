package com.gmc.retreat.registration.dto;

import jakarta.validation.constraints.NotNull;

public record AdminRegistrationFeePaidUpdateRequest(
        @NotNull Boolean feePaid
) {
}

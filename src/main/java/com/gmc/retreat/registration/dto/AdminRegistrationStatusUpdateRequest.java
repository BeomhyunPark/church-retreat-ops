package com.gmc.retreat.registration.dto;

import com.gmc.retreat.registration.domain.RegistrationStatus;
import jakarta.validation.constraints.NotNull;

public record AdminRegistrationStatusUpdateRequest(
        @NotNull RegistrationStatus status
) {
}

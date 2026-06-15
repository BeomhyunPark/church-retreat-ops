package com.gmc.retreat.registration.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminRegistrationManagementUpdateRequest(
        @Size(max = 2000) String adminMemo,
        @NotNull Boolean newcomer,
        @NotNull Boolean careTarget
) {
}

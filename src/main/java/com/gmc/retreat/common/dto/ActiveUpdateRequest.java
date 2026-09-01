package com.gmc.retreat.common.dto;

import jakarta.validation.constraints.NotNull;

public record ActiveUpdateRequest(
        @NotNull Boolean active
) {
}

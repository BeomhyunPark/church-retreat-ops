package com.gmc.retreat.community.dto;

import jakarta.validation.constraints.NotNull;

public record ActiveUpdateRequest(
        @NotNull Boolean active
) {
}

package com.gmc.retreat.admin.dto;

import com.gmc.retreat.admin.domain.AdminStatus;
import jakarta.validation.constraints.NotNull;

public record AdminStatusUpdateRequest(
        @NotNull AdminStatus status
) {
}

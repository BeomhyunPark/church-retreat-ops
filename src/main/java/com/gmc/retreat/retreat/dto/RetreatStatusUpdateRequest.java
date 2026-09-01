package com.gmc.retreat.retreat.dto;

import com.gmc.retreat.retreat.domain.RetreatStatus;
import jakarta.validation.constraints.NotNull;

public record RetreatStatusUpdateRequest(
        @NotNull RetreatStatus status
) {
}

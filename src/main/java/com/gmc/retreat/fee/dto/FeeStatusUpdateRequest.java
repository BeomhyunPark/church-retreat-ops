package com.gmc.retreat.fee.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FeeStatusUpdateRequest(
        @NotNull Boolean feePaid,
        @Size(max = 500) String reason
) {
}

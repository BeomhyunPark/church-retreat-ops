package com.gmc.retreat.retreat.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RetreatGroupMemberOrderRequest(
        @NotNull List<@Valid @NotNull Long> participantIds
) {
}

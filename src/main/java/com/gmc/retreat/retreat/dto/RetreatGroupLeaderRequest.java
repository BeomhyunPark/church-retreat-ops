package com.gmc.retreat.retreat.dto;

import jakarta.validation.constraints.NotNull;

public record RetreatGroupLeaderRequest(
        @NotNull Long participantId
) {
}

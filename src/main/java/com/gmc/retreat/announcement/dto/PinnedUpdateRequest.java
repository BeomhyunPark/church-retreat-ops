package com.gmc.retreat.announcement.dto;

import jakarta.validation.constraints.NotNull;

public record PinnedUpdateRequest(
        @NotNull Boolean pinned
) {
}

package com.gmc.retreat.announcement.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;

public record AnnouncementRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 10000) String content,
        @NotNull Boolean pinned,
        @NotNull Boolean active,
        OffsetDateTime visibleFrom,
        OffsetDateTime visibleUntil,
        @NotEmpty List<@Valid AnnouncementTargetRequest> targets
) {
}

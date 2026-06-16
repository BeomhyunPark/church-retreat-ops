package com.gmc.retreat.announcement.dto;

import com.gmc.retreat.announcement.domain.AnnouncementTargetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AnnouncementTargetRequest(
        @NotNull AnnouncementTargetType targetType,
        @Size(max = 100) String targetValue
) {
}

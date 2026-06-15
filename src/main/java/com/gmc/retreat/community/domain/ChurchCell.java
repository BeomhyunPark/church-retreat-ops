package com.gmc.retreat.community.domain;

import java.time.OffsetDateTime;

public record ChurchCell(
        Long id,
        Long middleGroupId,
        String middleGroupName,
        String name,
        String cellLeaderName,
        String description,
        Integer displayOrder,
        Boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

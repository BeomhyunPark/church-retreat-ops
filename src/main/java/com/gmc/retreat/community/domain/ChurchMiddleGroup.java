package com.gmc.retreat.community.domain;

import java.time.OffsetDateTime;

public record ChurchMiddleGroup(
        Long id,
        String name,
        String elderName,
        String description,
        Integer displayOrder,
        Boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

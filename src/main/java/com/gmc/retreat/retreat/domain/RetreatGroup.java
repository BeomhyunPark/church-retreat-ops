package com.gmc.retreat.retreat.domain;

import java.time.OffsetDateTime;

public record RetreatGroup(
        Long id,
        String name,
        String description,
        Integer displayOrder,
        Boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

package com.gmc.retreat.retreat.dto;

import com.gmc.retreat.retreat.domain.RetreatGroup;
import java.time.OffsetDateTime;

public record RetreatGroupResponse(
        Long id,
        String name,
        String description,
        Integer displayOrder,
        Boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static RetreatGroupResponse from(RetreatGroup group) {
        return new RetreatGroupResponse(
                group.id(),
                group.name(),
                group.description(),
                group.displayOrder(),
                group.active(),
                group.createdAt(),
                group.updatedAt()
        );
    }
}

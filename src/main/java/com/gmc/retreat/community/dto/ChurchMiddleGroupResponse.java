package com.gmc.retreat.community.dto;

import com.gmc.retreat.community.domain.ChurchMiddleGroup;
import java.time.OffsetDateTime;

public record ChurchMiddleGroupResponse(
        Long id,
        String name,
        String elderName,
        String description,
        Integer displayOrder,
        Boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ChurchMiddleGroupResponse from(ChurchMiddleGroup middleGroup) {
        return new ChurchMiddleGroupResponse(
                middleGroup.id(),
                middleGroup.name(),
                middleGroup.elderName(),
                middleGroup.description(),
                middleGroup.displayOrder(),
                middleGroup.active(),
                middleGroup.createdAt(),
                middleGroup.updatedAt()
        );
    }
}

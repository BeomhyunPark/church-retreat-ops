package com.gmc.retreat.community.dto;

import com.gmc.retreat.community.domain.ChurchCell;
import java.time.OffsetDateTime;

public record ChurchCellResponse(
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
    public static ChurchCellResponse from(ChurchCell cell) {
        return new ChurchCellResponse(
                cell.id(),
                cell.middleGroupId(),
                cell.middleGroupName(),
                cell.name(),
                cell.cellLeaderName(),
                cell.description(),
                cell.displayOrder(),
                cell.active(),
                cell.createdAt(),
                cell.updatedAt()
        );
    }
}

package com.gmc.retreat.community.dto;

import java.util.List;

public record CommunityTreeResponse(
        List<MiddleGroupNode> middleGroups
) {
    public record MiddleGroupNode(
            Long id,
            String name,
            String elderName,
            String description,
            Integer displayOrder,
            Boolean active,
            List<CellNode> cells
    ) {
    }

    public record CellNode(
            Long id,
            String name,
            String cellLeaderName,
            String description,
            Integer displayOrder,
            Boolean active
    ) {
    }
}

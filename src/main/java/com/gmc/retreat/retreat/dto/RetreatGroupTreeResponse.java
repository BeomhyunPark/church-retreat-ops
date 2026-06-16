package com.gmc.retreat.retreat.dto;

import java.util.List;

public record RetreatGroupTreeResponse(
        List<GroupNode> groups
) {
    public record GroupNode(
            Long id,
            String name,
            String description,
            Integer displayOrder,
            Boolean active,
            List<MemberNode> members
    ) {
    }

    public record MemberNode(
            Long id,
            Long participantId,
            String participantName,
            Boolean leader
    ) {
    }
}

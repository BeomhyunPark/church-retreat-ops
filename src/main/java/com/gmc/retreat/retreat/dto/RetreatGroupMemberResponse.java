package com.gmc.retreat.retreat.dto;

import com.gmc.retreat.registration.domain.Gender;
import com.gmc.retreat.retreat.domain.RetreatGroupMember;
import java.time.OffsetDateTime;

public record RetreatGroupMemberResponse(
        Long id,
        Long retreatGroupId,
        String retreatGroupName,
        Long participantId,
        String participantName,
        Gender gender,
        Integer birthYear,
        String middleGroupName,
        String cellName,
        Boolean leader,
        Integer displayOrder,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static RetreatGroupMemberResponse from(RetreatGroupMember member) {
        return new RetreatGroupMemberResponse(
                member.id(),
                member.retreatGroupId(),
                member.retreatGroupName(),
                member.participantId(),
                member.participantName(),
                member.gender(),
                member.birthYear(),
                member.middleGroupName(),
                member.cellName(),
                member.leader(),
                member.displayOrder(),
                member.createdAt(),
                member.updatedAt()
        );
    }
}

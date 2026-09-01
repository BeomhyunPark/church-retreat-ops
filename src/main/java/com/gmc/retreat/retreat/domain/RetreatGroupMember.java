package com.gmc.retreat.retreat.domain;

import com.gmc.retreat.registration.domain.Gender;
import java.time.OffsetDateTime;

public record RetreatGroupMember(
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
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

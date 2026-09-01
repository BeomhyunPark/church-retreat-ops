package com.gmc.retreat.fee.domain;

import com.gmc.retreat.registration.domain.Gender;
import java.time.OffsetDateTime;

public record FeeRosterItem(
        Long participantId,
        String name,
        Gender gender,
        Integer birthYear,
        String phoneLast4,
        String middleGroupName,
        String cellName,
        Long retreatGroupId,
        String retreatGroupName,
        Boolean retreatGroupLeader,
        Boolean feePaid,
        OffsetDateTime feeStatusUpdatedAt,
        Long feeStatusUpdatedByAdminId,
        String feeStatusUpdatedByAdminName
) {
}

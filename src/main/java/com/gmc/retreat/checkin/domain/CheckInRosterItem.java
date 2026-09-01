package com.gmc.retreat.checkin.domain;

import com.gmc.retreat.registration.domain.Gender;
import java.time.OffsetDateTime;

public record CheckInRosterItem(
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
        Boolean checkedIn,
        OffsetDateTime checkedInAt,
        CheckInMethod checkInMethod,
        Long checkedInByAdminId,
        String checkedInByAdminName,
        OffsetDateTime cancelledAt,
        Long cancelledByAdminId,
        String cancelledByAdminName
) {
}

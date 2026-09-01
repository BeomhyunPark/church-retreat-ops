package com.gmc.retreat.checkin.dto;

import com.gmc.retreat.checkin.domain.CheckInMethod;
import com.gmc.retreat.checkin.domain.CheckInRosterItem;
import com.gmc.retreat.registration.domain.Gender;
import java.time.OffsetDateTime;

public record CheckInRosterResponse(
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
        CheckInAdminActorResponse checkedInBy,
        OffsetDateTime cancelledAt,
        CheckInAdminActorResponse cancelledBy
) {
    public static CheckInRosterResponse from(CheckInRosterItem item) {
        return new CheckInRosterResponse(
                item.participantId(),
                item.name(),
                item.gender(),
                item.birthYear(),
                item.phoneLast4(),
                item.middleGroupName(),
                item.cellName(),
                item.retreatGroupId(),
                item.retreatGroupName(),
                item.retreatGroupLeader(),
                item.checkedIn(),
                item.checkedInAt(),
                item.checkInMethod(),
                actor(item.checkedInByAdminId(), item.checkedInByAdminName()),
                item.cancelledAt(),
                actor(item.cancelledByAdminId(), item.cancelledByAdminName())
        );
    }

    private static CheckInAdminActorResponse actor(Long id, String name) {
        if (id == null) {
            return null;
        }
        return new CheckInAdminActorResponse(id, name);
    }
}

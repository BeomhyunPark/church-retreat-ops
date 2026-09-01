package com.gmc.retreat.fee.dto;

import com.gmc.retreat.fee.domain.FeeRosterItem;
import com.gmc.retreat.registration.domain.Gender;
import java.time.OffsetDateTime;

public record FeeRosterResponse(
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
        FeeAdminActorResponse feeStatusUpdatedBy
) {
    public static FeeRosterResponse from(FeeRosterItem item) {
        return new FeeRosterResponse(
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
                item.feePaid(),
                item.feeStatusUpdatedAt(),
                actor(item.feeStatusUpdatedByAdminId(), item.feeStatusUpdatedByAdminName())
        );
    }

    static FeeAdminActorResponse actor(Long id, String name) {
        if (id == null) {
            return null;
        }
        return new FeeAdminActorResponse(id, name);
    }
}

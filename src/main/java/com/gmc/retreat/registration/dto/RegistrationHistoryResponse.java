package com.gmc.retreat.registration.dto;

import com.gmc.retreat.registration.domain.RegistrationActorType;
import com.gmc.retreat.registration.domain.RegistrationHistory;
import com.gmc.retreat.registration.domain.RegistrationHistoryChangeType;
import java.time.OffsetDateTime;

public record RegistrationHistoryResponse(
        Long id,
        Long registrationId,
        RegistrationHistoryChangeType changeType,
        String previousSnapshotJson,
        String newSnapshotJson,
        RegistrationActorType actorType,
        Long actorAdminUserId,
        OffsetDateTime createdAt
) {
    public static RegistrationHistoryResponse from(RegistrationHistory history) {
        return new RegistrationHistoryResponse(
                history.id(),
                history.registrationId(),
                history.changeType(),
                history.previousSnapshotJson(),
                history.newSnapshotJson(),
                history.actorType(),
                history.actorAdminUserId(),
                history.createdAt()
        );
    }
}

package com.gmc.retreat.registration.mapper;

import com.gmc.retreat.registration.domain.RegistrationActorType;
import com.gmc.retreat.registration.domain.RegistrationHistoryChangeType;

public record RegistrationHistoryInsert(
        Long registrationId,
        RegistrationHistoryChangeType changeType,
        String previousSnapshotJson,
        String newSnapshotJson,
        RegistrationActorType actorType,
        Long actorAdminUserId
) {
}

package com.gmc.retreat.registration.domain;

import java.time.OffsetDateTime;

public record RegistrationHistory(
        Long id,
        Long registrationId,
        RegistrationHistoryChangeType changeType,
        String previousSnapshotJson,
        String newSnapshotJson,
        RegistrationActorType actorType,
        Long actorAdminUserId,
        OffsetDateTime createdAt
) {
}

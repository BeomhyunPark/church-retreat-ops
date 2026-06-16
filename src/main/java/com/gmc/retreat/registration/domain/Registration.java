package com.gmc.retreat.registration.domain;

import java.time.OffsetDateTime;

public record Registration(
        Long id,
        String name,
        String normalizedName,
        Gender gender,
        Integer birthYear,
        String phoneNumber,
        String phoneLastFour,
        String churchCellDepartment,
        Long churchCellId,
        String churchCellName,
        Long middleGroupId,
        String middleGroupName,
        Long retreatGroupId,
        String retreatGroupName,
        Boolean retreatGroupLeader,
        String lookupKeyHash,
        Boolean privacyConsentAgreed,
        Boolean feePaid,
        RegistrationStatus status,
        String adminMemo,
        Boolean newcomer,
        Boolean careTarget,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

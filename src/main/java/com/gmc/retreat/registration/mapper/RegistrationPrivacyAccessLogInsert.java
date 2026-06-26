package com.gmc.retreat.registration.mapper;

public record RegistrationPrivacyAccessLogInsert(
        Long registrationId,
        Long adminUserId,
        String accessType,
        String sensitiveFields
) {
}

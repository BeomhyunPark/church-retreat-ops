package com.gmc.retreat.registration.dto;

import com.gmc.retreat.registration.domain.Gender;
import com.gmc.retreat.registration.domain.Registration;
import com.gmc.retreat.registration.domain.RegistrationStatus;
import com.gmc.retreat.registration.service.PhoneNumberNormalizer;
import java.time.OffsetDateTime;

public record AdminRegistrationResponse(
        Long id,
        String name,
        Gender gender,
        Integer birthYear,
        String phoneNumber,
        String churchCellDepartment,
        Boolean feePaid,
        RegistrationStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static AdminRegistrationResponse listItem(Registration registration) {
        return from(registration, true);
    }

    public static AdminRegistrationResponse detail(Registration registration) {
        return from(registration, false);
    }

    private static AdminRegistrationResponse from(Registration registration, boolean maskPhone) {
        return new AdminRegistrationResponse(
                registration.id(),
                registration.name(),
                registration.gender(),
                registration.birthYear(),
                maskPhone ? PhoneNumberNormalizer.mask(registration.phoneNumber()) : registration.phoneNumber(),
                registration.churchCellDepartment(),
                registration.feePaid(),
                registration.status(),
                registration.createdAt(),
                registration.updatedAt()
        );
    }
}

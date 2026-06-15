package com.gmc.retreat.registration.dto;

import com.gmc.retreat.registration.domain.Gender;
import com.gmc.retreat.registration.domain.Registration;
import com.gmc.retreat.registration.domain.RegistrationStatus;
import com.gmc.retreat.registration.service.PhoneNumberNormalizer;

public record RegistrationResponse(
        Long id,
        String name,
        Gender gender,
        Integer birthYear,
        String phoneNumber,
        String churchCellDepartment,
        Boolean feePaid,
        RegistrationStatus status
) {
    public static RegistrationResponse from(Registration registration) {
        return new RegistrationResponse(
                registration.id(),
                registration.name(),
                registration.gender(),
                registration.birthYear(),
                PhoneNumberNormalizer.mask(registration.phoneNumber()),
                registration.churchCellDepartment(),
                registration.feePaid(),
                registration.status()
        );
    }
}

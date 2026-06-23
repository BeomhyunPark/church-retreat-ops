package com.gmc.retreat.registration.dto;

import com.gmc.retreat.registration.domain.Gender;
import com.gmc.retreat.registration.domain.AttendanceSlot;
import com.gmc.retreat.registration.domain.AttendanceType;
import com.gmc.retreat.registration.domain.Registration;
import com.gmc.retreat.registration.domain.RegistrationStatus;
import com.gmc.retreat.registration.domain.TransportationType;
import com.gmc.retreat.registration.service.PhoneNumberNormalizer;
import java.util.Arrays;
import java.util.List;

public record RegistrationResponse(
        Long id,
        String name,
        Gender gender,
        Integer birthYear,
        String phoneNumber,
        String churchCellDepartment,
        AttendanceType attendanceType,
        List<AttendanceSlot> attendanceSlots,
        TransportationType transportationType,
        Boolean carpoolNeeded,
        Boolean carpoolOffer,
        Integer carpoolSeats,
        String transportationNote,
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
                registration.attendanceType(),
                parseAttendanceSlots(registration.attendanceSlots()),
                registration.transportationType(),
                registration.carpoolNeeded(),
                registration.carpoolOffer(),
                registration.carpoolSeats(),
                registration.transportationNote(),
                registration.feePaid(),
                registration.status()
        );
    }

    private static List<AttendanceSlot> parseAttendanceSlots(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(AttendanceSlot::valueOf)
                .toList();
    }
}

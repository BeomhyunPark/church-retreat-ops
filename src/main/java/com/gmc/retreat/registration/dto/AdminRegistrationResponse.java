package com.gmc.retreat.registration.dto;

import com.gmc.retreat.registration.domain.Gender;
import com.gmc.retreat.registration.domain.AttendanceSlot;
import com.gmc.retreat.registration.domain.AttendanceType;
import com.gmc.retreat.registration.domain.Registration;
import com.gmc.retreat.registration.domain.RegistrationStatus;
import com.gmc.retreat.registration.domain.TransportationType;
import com.gmc.retreat.registration.service.PhoneNumberNormalizer;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

public record AdminRegistrationResponse(
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
        Long churchCellId,
        String churchCellName,
        Long middleGroupId,
        String middleGroupName,
        Long retreatGroupId,
        String retreatGroupName,
        Boolean retreatGroupLeader,
        Boolean feePaid,
        RegistrationStatus status,
        String adminMemo,
        Boolean newcomer,
        Boolean careTarget,
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
                registration.attendanceType(),
                parseAttendanceSlots(registration.attendanceSlots()),
                registration.transportationType(),
                registration.carpoolNeeded(),
                registration.carpoolOffer(),
                registration.carpoolSeats(),
                registration.transportationNote(),
                registration.churchCellId(),
                registration.churchCellName(),
                registration.middleGroupId(),
                registration.middleGroupName(),
                registration.retreatGroupId(),
                registration.retreatGroupName(),
                registration.retreatGroupLeader(),
                registration.feePaid(),
                registration.status(),
                registration.adminMemo(),
                registration.newcomer(),
                registration.careTarget(),
                registration.createdAt(),
                registration.updatedAt()
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

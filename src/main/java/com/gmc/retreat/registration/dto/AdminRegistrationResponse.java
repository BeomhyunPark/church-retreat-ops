package com.gmc.retreat.registration.dto;

import com.gmc.retreat.registration.domain.AttendanceType;
import com.gmc.retreat.registration.domain.Gender;
import com.gmc.retreat.registration.domain.Registration;
import com.gmc.retreat.registration.domain.RegistrationStatus;
import com.gmc.retreat.registration.domain.TransportationMethod;
import com.gmc.retreat.registration.domain.WorshipBusRideSlot;
import com.gmc.retreat.registration.service.PhoneNumberNormalizer;
import java.time.OffsetDateTime;

public record AdminRegistrationResponse(
        Long id,
        String name,
        Gender gender,
        Integer birthYear,
        String phoneNumber,
        String churchCellDepartment,
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
        Boolean checkedIn,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        AttendanceType attendanceType,
        OffsetDateTime plannedArrivalAt,
        OffsetDateTime plannedDepartureAt,
        String partialAttendanceNote,
        Boolean lodgingNight1,
        Boolean lodgingNight2,
        Boolean attendDay1Morning,
        Boolean attendDay1Afternoon,
        Boolean attendDay1Worship,
        Boolean attendDay2Morning,
        Boolean attendDay2Afternoon,
        Boolean attendDay2Worship,
        Boolean attendDay3Morning,
        Boolean attendDay3Afternoon,
        TransportationMethod inboundTransportationMethod,
        Boolean inboundCarpoolAvailable,
        Integer inboundCarpoolSeats,
        String inboundCarpoolArea,
        String inboundCarpoolRouteArea,
        String inboundCarpoolNote,
        String inboundCarpoolPreferredArea,
        String inboundCarpoolPreferredNote,
        WorshipBusRideSlot inboundWorshipBusRideSlot,
        TransportationMethod outboundTransportationMethod,
        Boolean outboundCarpoolAvailable,
        Integer outboundCarpoolSeats,
        String outboundCarpoolArea,
        String outboundCarpoolRouteArea,
        String outboundCarpoolNote,
        String outboundCarpoolPreferredArea,
        String outboundCarpoolPreferredNote,
        WorshipBusRideSlot outboundWorshipBusRideSlot
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
                registration.checkedIn(),
                registration.createdAt(),
                registration.updatedAt(),
                registration.attendanceType(),
                maskPhone ? null : registration.plannedArrivalAt(),
                maskPhone ? null : registration.plannedDepartureAt(),
                maskPhone ? null : registration.partialAttendanceNote(),
                registration.lodgingNight1(),
                registration.lodgingNight2(),
                registration.attendDay1Morning(),
                registration.attendDay1Afternoon(),
                registration.attendDay1Worship(),
                registration.attendDay2Morning(),
                registration.attendDay2Afternoon(),
                registration.attendDay2Worship(),
                registration.attendDay3Morning(),
                registration.attendDay3Afternoon(),
                registration.inboundTransportationMethod(),
                maskPhone ? null : registration.inboundCarpoolAvailable(),
                maskPhone ? null : registration.inboundCarpoolSeats(),
                maskPhone ? null : registration.inboundCarpoolArea(),
                maskPhone ? null : registration.inboundCarpoolRouteArea(),
                maskPhone ? null : registration.inboundCarpoolNote(),
                maskPhone ? null : registration.inboundCarpoolPreferredArea(),
                maskPhone ? null : registration.inboundCarpoolPreferredNote(),
                registration.inboundWorshipBusRideSlot(),
                registration.outboundTransportationMethod(),
                maskPhone ? null : registration.outboundCarpoolAvailable(),
                maskPhone ? null : registration.outboundCarpoolSeats(),
                maskPhone ? null : registration.outboundCarpoolArea(),
                maskPhone ? null : registration.outboundCarpoolRouteArea(),
                maskPhone ? null : registration.outboundCarpoolNote(),
                maskPhone ? null : registration.outboundCarpoolPreferredArea(),
                maskPhone ? null : registration.outboundCarpoolPreferredNote(),
                registration.outboundWorshipBusRideSlot()
        );
    }
}

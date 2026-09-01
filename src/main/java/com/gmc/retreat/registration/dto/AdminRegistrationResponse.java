package com.gmc.retreat.registration.dto;

import com.gmc.retreat.registration.domain.AttendanceType;
import com.gmc.retreat.registration.domain.Gender;
import com.gmc.retreat.registration.domain.Registration;
import com.gmc.retreat.registration.domain.RegistrationStatus;
import com.gmc.retreat.registration.domain.TransportationMethod;
import com.gmc.retreat.registration.domain.WorshipBusRideSlot;
import com.gmc.retreat.registration.service.PhoneNumberNormalizer;
import java.time.OffsetDateTime;
import java.util.List;

public record AdminRegistrationResponse(
        Long id,
        String name,
        Gender gender,
        Integer birthYear,
        String phoneNumber,
        String middleGroupName,
        String cellName,
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
        OffsetDateTime participantUpdatedAt,
        AttendanceType attendanceType,
        OffsetDateTime plannedArrivalAt,
        OffsetDateTime plannedDepartureAt,
        String partialAttendanceNote,
        Boolean lodgingNight1,
        Boolean lodgingNight2,
        List<Long> selectedOptionIds,
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
    public static AdminRegistrationResponse listItem(Registration registration, List<Long> selectedOptionIds) {
        return from(registration, selectedOptionIds, true);
    }

    public static AdminRegistrationResponse detail(Registration registration, List<Long> selectedOptionIds) {
        return from(registration, selectedOptionIds, false);
    }

    private static AdminRegistrationResponse from(
            Registration registration,
            List<Long> selectedOptionIds,
            boolean maskPhone
    ) {
        return new AdminRegistrationResponse(
                registration.id(),
                registration.name(),
                registration.gender(),
                registration.birthYear(),
                maskPhone ? PhoneNumberNormalizer.mask(registration.phoneNumber()) : registration.phoneNumber(),
                registration.middleGroupName(),
                registration.cellName(),
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
                registration.participantUpdatedAt(),
                registration.attendanceType(),
                maskPhone ? null : registration.plannedArrivalAt(),
                maskPhone ? null : registration.plannedDepartureAt(),
                maskPhone ? null : registration.partialAttendanceNote(),
                registration.lodgingNight1(),
                registration.lodgingNight2(),
                List.copyOf(selectedOptionIds),
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

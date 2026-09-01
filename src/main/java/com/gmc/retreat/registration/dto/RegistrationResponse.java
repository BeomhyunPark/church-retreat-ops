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

public record RegistrationResponse(
        Long id,
        String name,
        Gender gender,
        Integer birthYear,
        String phoneNumber,
        String middleGroupName,
        String cellName,
        Boolean feePaid,
        RegistrationStatus status,
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
    public static RegistrationResponse from(Registration registration, List<Long> selectedOptionIds) {
        return new RegistrationResponse(
                registration.id(),
                registration.name(),
                registration.gender(),
                registration.birthYear(),
                PhoneNumberNormalizer.mask(registration.phoneNumber()),
                registration.middleGroupName(),
                registration.cellName(),
                registration.feePaid(),
                registration.status(),
                registration.attendanceType(),
                registration.plannedArrivalAt(),
                registration.plannedDepartureAt(),
                registration.partialAttendanceNote(),
                registration.lodgingNight1(),
                registration.lodgingNight2(),
                List.copyOf(selectedOptionIds),
                registration.inboundTransportationMethod(),
                registration.inboundCarpoolAvailable(),
                registration.inboundCarpoolSeats(),
                registration.inboundCarpoolArea(),
                registration.inboundCarpoolRouteArea(),
                registration.inboundCarpoolNote(),
                registration.inboundCarpoolPreferredArea(),
                registration.inboundCarpoolPreferredNote(),
                registration.inboundWorshipBusRideSlot(),
                registration.outboundTransportationMethod(),
                registration.outboundCarpoolAvailable(),
                registration.outboundCarpoolSeats(),
                registration.outboundCarpoolArea(),
                registration.outboundCarpoolRouteArea(),
                registration.outboundCarpoolNote(),
                registration.outboundCarpoolPreferredArea(),
                registration.outboundCarpoolPreferredNote(),
                registration.outboundWorshipBusRideSlot()
        );
    }
}

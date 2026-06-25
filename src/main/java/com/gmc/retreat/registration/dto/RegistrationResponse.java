package com.gmc.retreat.registration.dto;

import com.gmc.retreat.registration.domain.AttendanceType;
import com.gmc.retreat.registration.domain.Gender;
import com.gmc.retreat.registration.domain.Registration;
import com.gmc.retreat.registration.domain.RegistrationStatus;
import com.gmc.retreat.registration.domain.TransportationMethod;
import com.gmc.retreat.registration.service.PhoneNumberNormalizer;

public record RegistrationResponse(
        Long id,
        String name,
        Gender gender,
        Integer birthYear,
        String phoneNumber,
        String churchCellDepartment,
        Boolean feePaid,
        RegistrationStatus status,
        AttendanceType attendanceType,
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
        String inboundCarpoolPreferredArea,
        TransportationMethod outboundTransportationMethod,
        Boolean outboundCarpoolAvailable,
        Integer outboundCarpoolSeats,
        String outboundCarpoolArea,
        String outboundCarpoolPreferredArea
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
                registration.status(),
                registration.attendanceType(),
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
                registration.inboundCarpoolAvailable(),
                registration.inboundCarpoolSeats(),
                registration.inboundCarpoolArea(),
                registration.inboundCarpoolPreferredArea(),
                registration.outboundTransportationMethod(),
                registration.outboundCarpoolAvailable(),
                registration.outboundCarpoolSeats(),
                registration.outboundCarpoolArea(),
                registration.outboundCarpoolPreferredArea()
        );
    }
}

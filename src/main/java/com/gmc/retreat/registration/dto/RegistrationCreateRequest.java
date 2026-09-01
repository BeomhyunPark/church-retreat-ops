package com.gmc.retreat.registration.dto;

import com.gmc.retreat.registration.domain.AttendanceType;
import com.gmc.retreat.registration.domain.Gender;
import com.gmc.retreat.registration.domain.TransportationMethod;
import com.gmc.retreat.registration.domain.WorshipBusRideSlot;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;

public record RegistrationCreateRequest(
        @NotBlank @Size(max = 50) String name,
        @NotNull Gender gender,
        @NotNull @Min(1900) @Max(2026) Integer birthYear,
        @NotBlank String phoneNumber,
        @Size(max = 100) String middleGroupName,
        @Size(max = 100) String cellName,
        @NotNull @AssertTrue Boolean privacyConsentAgreed,
        @NotBlank @Pattern(regexp = "^[0-9]{6}$") String lookupKey,
        @NotNull AttendanceType attendanceType,
        Boolean lodgingNight1,
        Boolean lodgingNight2,
        @NotNull List<@NotNull Long> selectedOptionIds,
        OffsetDateTime plannedArrivalAt,
        OffsetDateTime plannedDepartureAt,
        @Size(max = 300) String partialAttendanceNote,
        @NotNull TransportationMethod inboundTransportationMethod,
        Boolean inboundCarpoolAvailable,
        @Min(1) @Max(10) Integer inboundCarpoolSeats,
        @Size(max = 100) String inboundCarpoolArea,
        @Size(max = 100) String inboundCarpoolRouteArea,
        @Size(max = 200) String inboundCarpoolNote,
        @Size(max = 100) String inboundCarpoolPreferredArea,
        @Size(max = 200) String inboundCarpoolPreferredNote,
        WorshipBusRideSlot inboundWorshipBusRideSlot,
        @NotNull TransportationMethod outboundTransportationMethod,
        Boolean outboundCarpoolAvailable,
        @Min(1) @Max(10) Integer outboundCarpoolSeats,
        @Size(max = 100) String outboundCarpoolArea,
        @Size(max = 100) String outboundCarpoolRouteArea,
        @Size(max = 200) String outboundCarpoolNote,
        @Size(max = 100) String outboundCarpoolPreferredArea,
        @Size(max = 200) String outboundCarpoolPreferredNote,
        WorshipBusRideSlot outboundWorshipBusRideSlot
) {
}

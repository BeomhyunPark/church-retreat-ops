package com.gmc.retreat.registration.dto;

import com.gmc.retreat.registration.domain.Gender;
import com.gmc.retreat.registration.domain.AttendanceSlot;
import com.gmc.retreat.registration.domain.AttendanceType;
import com.gmc.retreat.registration.domain.TransportationType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record RegistrationCreateRequest(
        @NotBlank @Size(max = 50) String name,
        @NotNull Gender gender,
        @NotNull @Min(1900) @Max(2026) Integer birthYear,
        @NotBlank String phoneNumber,
        @Size(max = 100) String churchCellDepartment,
        @NotNull AttendanceType attendanceType,
        List<AttendanceSlot> attendanceSlots,
        @NotNull TransportationType transportationType,
        @NotNull Boolean carpoolNeeded,
        @NotNull Boolean carpoolOffer,
        @Min(0) @Max(20) Integer carpoolSeats,
        @Size(max = 200) String transportationNote,
        @NotNull @AssertTrue Boolean privacyConsentAgreed
) {
    @AssertTrue
    public boolean isPartialPlanValid() {
        if (attendanceType != AttendanceType.PARTIAL) {
            return true;
        }
        return attendanceSlots != null && !attendanceSlots.isEmpty();
    }

    @AssertTrue
    public boolean isCarpoolOfferValid() {
        if (Boolean.TRUE.equals(carpoolOffer)) {
            return transportationType == TransportationType.OWN_CAR;
        }
        return true;
    }

}

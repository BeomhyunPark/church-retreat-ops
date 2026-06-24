package com.gmc.retreat.registration.dto;

import com.gmc.retreat.registration.domain.AttendanceType;
import com.gmc.retreat.registration.domain.Gender;
import com.gmc.retreat.registration.domain.TransportationMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistrationSelfUpdateRequest(
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Pattern(regexp = "^[0-9]{4}$") String phoneLastFour,
        @NotBlank @Pattern(regexp = "^[0-9]{6}$") String lookupKey,
        @Valid @NotNull Update update
) {
    public record Update(
            @NotNull Gender gender,
            @NotNull @Min(1900) @Max(2026) Integer birthYear,
            @NotBlank String phoneNumber,
            @Size(max = 100) String churchCellDepartment,
            @NotNull AttendanceType attendanceType,
            @NotNull TransportationMethod transportation,
            Boolean carpoolAvailable,
            @Min(1) @Max(10) Integer carpoolSeats,
            Boolean lodgingNight1,
            Boolean lodgingNight2,
            Boolean attendDay1Morning,
            Boolean attendDay1Afternoon,
            Boolean attendDay1Worship,
            Boolean attendDay2Morning,
            Boolean attendDay2Afternoon,
            Boolean attendDay2Worship,
            Boolean attendDay3Morning,
            Boolean attendDay3Afternoon
    ) {
    }
}

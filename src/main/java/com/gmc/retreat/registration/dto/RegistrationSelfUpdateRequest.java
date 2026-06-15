package com.gmc.retreat.registration.dto;

import com.gmc.retreat.registration.domain.Gender;
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
        @NotBlank String lookupKey,
        @Valid @NotNull Update update
) {
    public record Update(
            @NotNull Gender gender,
            @NotNull @Min(1900) @Max(2026) Integer birthYear,
            @NotBlank String phoneNumber,
            @Size(max = 100) String churchCellDepartment
    ) {
    }
}

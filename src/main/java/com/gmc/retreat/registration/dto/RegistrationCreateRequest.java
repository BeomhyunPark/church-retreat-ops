package com.gmc.retreat.registration.dto;

import com.gmc.retreat.registration.domain.Gender;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistrationCreateRequest(
        @NotBlank @Size(max = 50) String name,
        @NotNull Gender gender,
        @NotNull @Min(1900) @Max(2026) Integer birthYear,
        @NotBlank String phoneNumber,
        @Size(max = 100) String churchCellDepartment,
        @NotNull @AssertTrue Boolean privacyConsentAgreed
) {
}

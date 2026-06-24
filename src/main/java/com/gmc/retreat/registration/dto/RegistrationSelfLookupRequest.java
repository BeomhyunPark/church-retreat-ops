package com.gmc.retreat.registration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistrationSelfLookupRequest(
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Pattern(regexp = "^[0-9]{6}$") String lookupKey
) {
}

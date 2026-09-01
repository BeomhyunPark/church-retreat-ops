package com.gmc.retreat.retreat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record RetreatUpdateRequest(
        @NotBlank @Size(max = 150) String name,
        @NotNull LocalDate startsOn,
        @NotNull LocalDate endsOn
) {
}

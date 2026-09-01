package com.gmc.retreat.participation.dto;

import com.gmc.retreat.participation.domain.ParticipationOptionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ParticipationOptionRequest(
        @NotNull ParticipationOptionType optionType,
        @NotBlank @Size(max = 100) String label,
        @NotNull LocalDate eventDate,
        @NotNull @Min(0) Integer displayOrder,
        @NotNull Boolean active
) {
}

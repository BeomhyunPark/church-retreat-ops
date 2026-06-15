package com.gmc.retreat.community.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChurchMiddleGroupRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 100) String elderName,
        @Size(max = 2000) String description,
        @NotNull @Min(0) Integer displayOrder
) {
}

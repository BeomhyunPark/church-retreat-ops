package com.gmc.retreat.admin.dto;

import com.gmc.retreat.admin.domain.AdminRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminCreateRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotNull AdminRole role
) {
}

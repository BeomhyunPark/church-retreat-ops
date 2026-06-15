package com.gmc.retreat.admin.domain;

import java.time.OffsetDateTime;

public record AdminUser(
        Long id,
        String email,
        String passwordHash,
        String name,
        AdminRole role,
        AdminStatus status,
        OffsetDateTime lastLoginAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

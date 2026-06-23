package com.gmc.retreat.admin.dto;

import com.gmc.retreat.admin.domain.AdminRole;
import com.gmc.retreat.admin.domain.AdminStatus;
import com.gmc.retreat.admin.domain.AdminUser;
import java.time.OffsetDateTime;

public record AdminAccountResponse(
        Long id,
        String email,
        String name,
        AdminRole role,
        AdminStatus status,
        OffsetDateTime lastLoginAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static AdminAccountResponse from(AdminUser adminUser) {
        return new AdminAccountResponse(
                adminUser.id(),
                adminUser.email(),
                adminUser.name(),
                adminUser.role(),
                adminUser.status(),
                adminUser.lastLoginAt(),
                adminUser.createdAt(),
                adminUser.updatedAt()
        );
    }
}

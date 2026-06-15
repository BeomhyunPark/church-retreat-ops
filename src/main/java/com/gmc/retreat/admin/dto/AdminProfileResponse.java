package com.gmc.retreat.admin.dto;

import com.gmc.retreat.admin.domain.AdminRole;
import com.gmc.retreat.admin.domain.AdminStatus;
import com.gmc.retreat.admin.domain.AdminUser;

public record AdminProfileResponse(
        Long id,
        String email,
        String name,
        AdminRole role,
        AdminStatus status
) {
    public static AdminProfileResponse from(AdminUser adminUser) {
        return new AdminProfileResponse(
                adminUser.id(),
                adminUser.email(),
                adminUser.name(),
                adminUser.role(),
                adminUser.status()
        );
    }
}

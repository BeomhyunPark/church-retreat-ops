package com.gmc.retreat.admin.dto;

import com.gmc.retreat.admin.domain.AdminRole;
import com.gmc.retreat.admin.domain.AdminUser;

public record AdminSummaryResponse(
        Long id,
        String email,
        String name,
        AdminRole role
) {
    public static AdminSummaryResponse from(AdminUser adminUser) {
        return new AdminSummaryResponse(
                adminUser.id(),
                adminUser.email(),
                adminUser.name(),
                adminUser.role()
        );
    }
}

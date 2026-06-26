package com.gmc.retreat.admin.mapper;

import com.gmc.retreat.admin.domain.AdminRole;
import com.gmc.retreat.admin.domain.AdminStatus;

public record AdminUserInsert(
        String email,
        String passwordHash,
        String name,
        AdminRole role,
        AdminStatus status
) {
}

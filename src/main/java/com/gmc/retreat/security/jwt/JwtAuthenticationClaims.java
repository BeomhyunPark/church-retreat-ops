package com.gmc.retreat.security.jwt;

import com.gmc.retreat.admin.domain.AdminRole;

public record JwtAuthenticationClaims(
        Long adminUserId,
        String email,
        String name,
        AdminRole role
) {
}

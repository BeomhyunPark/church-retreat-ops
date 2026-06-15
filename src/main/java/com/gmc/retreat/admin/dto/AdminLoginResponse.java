package com.gmc.retreat.admin.dto;

public record AdminLoginResponse(
        String accessToken,
        String tokenType,
        AdminSummaryResponse admin
) {
}

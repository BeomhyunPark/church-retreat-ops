package com.gmc.retreat.admin.controller;

import com.gmc.retreat.admin.dto.AdminLoginRequest;
import com.gmc.retreat.admin.dto.AdminLoginResponse;
import com.gmc.retreat.admin.dto.AdminPasswordChangeRequest;
import com.gmc.retreat.admin.dto.AdminProfileResponse;
import com.gmc.retreat.admin.service.AdminAccountService;
import com.gmc.retreat.admin.service.AdminAuthService;
import com.gmc.retreat.api.ApiResponse;
import com.gmc.retreat.security.auth.AdminPrincipal;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;
    private final AdminAccountService adminAccountService;

    public AdminAuthController(AdminAuthService adminAuthService, AdminAccountService adminAccountService) {
        this.adminAuthService = adminAuthService;
        this.adminAccountService = adminAccountService;
    }

    @PostMapping("/login")
    public ApiResponse<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        return ApiResponse.success(adminAuthService.login(request.email(), request.password()));
    }

    @GetMapping("/me")
    public ApiResponse<AdminProfileResponse> me(Authentication authentication) {
        AdminPrincipal principal = (AdminPrincipal) authentication.getPrincipal();
        return ApiResponse.success(adminAuthService.getProfile(principal.id()));
    }

    @GetMapping("/me/preferences")
    public ApiResponse<Map<String, Object>> getPreferences(@AuthenticationPrincipal AdminPrincipal admin) {
        return ApiResponse.success(adminAccountService.getUiPreferences(admin.id()));
    }

    @PutMapping("/me/preferences")
    public ApiResponse<Void> updatePreferences(
            @AuthenticationPrincipal AdminPrincipal admin,
            @RequestBody Map<String, Object> preferences
    ) {
        adminAccountService.updateUiPreferences(admin.id(), preferences);
        return ApiResponse.success(null);
    }

    @PatchMapping("/password")
    public ApiResponse<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody AdminPasswordChangeRequest request
    ) {
        AdminPrincipal principal = (AdminPrincipal) authentication.getPrincipal();
        adminAuthService.changePassword(principal.id(), request);
        return ApiResponse.success(null);
    }
}

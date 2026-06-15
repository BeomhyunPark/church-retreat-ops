package com.gmc.retreat.admin.controller;

import com.gmc.retreat.admin.dto.AdminLoginRequest;
import com.gmc.retreat.admin.dto.AdminLoginResponse;
import com.gmc.retreat.admin.dto.AdminProfileResponse;
import com.gmc.retreat.admin.service.AdminAuthService;
import com.gmc.retreat.api.ApiResponse;
import com.gmc.retreat.security.auth.AdminPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
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
}

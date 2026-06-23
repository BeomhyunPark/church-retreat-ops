package com.gmc.retreat.admin.controller;

import com.gmc.retreat.admin.dto.AdminAccountResponse;
import com.gmc.retreat.admin.dto.AdminCreateRequest;
import com.gmc.retreat.admin.dto.AdminPasswordResetRequest;
import com.gmc.retreat.admin.dto.AdminStatusUpdateRequest;
import com.gmc.retreat.admin.dto.AdminUpdateRequest;
import com.gmc.retreat.admin.service.AdminAccountService;
import com.gmc.retreat.api.ApiResponse;
import com.gmc.retreat.security.auth.AdminPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    public AdminAccountController(AdminAccountService adminAccountService) {
        this.adminAccountService = adminAccountService;
    }

    @GetMapping
    public ApiResponse<List<AdminAccountResponse>> findAll(@AuthenticationPrincipal AdminPrincipal admin) {
        return ApiResponse.success(adminAccountService.findAll(admin));
    }

    @PostMapping
    public ApiResponse<AdminAccountResponse> create(
            @AuthenticationPrincipal AdminPrincipal admin,
            @Valid @RequestBody AdminCreateRequest request
    ) {
        return ApiResponse.success(adminAccountService.create(admin, request));
    }

    @PatchMapping("/{id}")
    public ApiResponse<AdminAccountResponse> update(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateRequest request
    ) {
        return ApiResponse.success(adminAccountService.update(admin, id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<AdminAccountResponse> updateStatus(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long id,
            @Valid @RequestBody AdminStatusUpdateRequest request
    ) {
        return ApiResponse.success(adminAccountService.updateStatus(admin, id, request));
    }

    @PatchMapping("/{id}/password")
    public ApiResponse<Void> resetPassword(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long id,
            @Valid @RequestBody AdminPasswordResetRequest request
    ) {
        adminAccountService.resetPassword(admin, id, request);
        return ApiResponse.success(null);
    }
}

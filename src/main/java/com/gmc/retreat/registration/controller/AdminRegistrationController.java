package com.gmc.retreat.registration.controller;

import java.util.List;

import com.gmc.retreat.api.ApiResponse;
import com.gmc.retreat.registration.domain.AttendanceType;
import com.gmc.retreat.registration.domain.RegistrationStatus;
import com.gmc.retreat.registration.dto.AdminRegistrationFeePaidUpdateRequest;
import com.gmc.retreat.registration.dto.AdminRegistrationManagementUpdateRequest;
import com.gmc.retreat.registration.dto.AdminRegistrationResponse;
import com.gmc.retreat.registration.dto.AdminRegistrationStatusUpdateRequest;
import com.gmc.retreat.registration.dto.PageResponse;
import com.gmc.retreat.registration.dto.RegistrationHistoryResponse;
import com.gmc.retreat.registration.service.RegistrationService;
import com.gmc.retreat.security.auth.AdminPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/registrations")
public class AdminRegistrationController {

    private final RegistrationService registrationService;

    public AdminRegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminRegistrationResponse>> list(
            @AuthenticationPrincipal AdminPrincipal admin,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) RegistrationStatus status,
            @RequestParam(required = false) Boolean feePaid,
            @RequestParam(required = false) Boolean newcomer,
            @RequestParam(required = false) Boolean careTarget,
            @RequestParam(required = false) Boolean checkedIn,
            @RequestParam(required = false) Boolean retreatGroupAssigned,
            @RequestParam(required = false) Boolean churchCellAssigned,
            @RequestParam(required = false) AttendanceType attendanceType,
            @RequestParam(required = false) String transportationNeed,
            @RequestParam(required = false) List<String> sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(registrationService.findRegistrations(
                admin,
                keyword,
                status,
                feePaid,
                newcomer,
                careTarget,
                checkedIn,
                retreatGroupAssigned,
                churchCellAssigned,
                attendanceType,
                transportationNeed,
                sort,
                page,
                size
        ));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminRegistrationResponse> detail(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long id
    ) {
        return ApiResponse.success(registrationService.findRegistration(admin, id));
    }

    @GetMapping("/{id}/histories")
    public ApiResponse<List<RegistrationHistoryResponse>> histories(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long id
    ) {
        return ApiResponse.success(registrationService.findHistories(admin, id));
    }

    @PatchMapping("/{id}/fee-paid")
    public ApiResponse<AdminRegistrationResponse> updateFeePaid(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long id,
            @Valid @RequestBody AdminRegistrationFeePaidUpdateRequest request
    ) {
        return ApiResponse.success(registrationService.updateFeePaid(admin, id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<AdminRegistrationResponse> updateStatus(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long id,
            @Valid @RequestBody AdminRegistrationStatusUpdateRequest request
    ) {
        return ApiResponse.success(registrationService.updateStatus(admin, id, request));
    }

    @PatchMapping("/{id}/management")
    public ApiResponse<AdminRegistrationResponse> updateManagement(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long id,
            @Valid @RequestBody AdminRegistrationManagementUpdateRequest request
    ) {
        return ApiResponse.success(registrationService.updateManagement(admin, id, request));
    }
}

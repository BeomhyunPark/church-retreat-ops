package com.gmc.retreat.registration.controller;

import com.gmc.retreat.api.ApiResponse;
import com.gmc.retreat.common.dto.DeleteConfirmationRequest;
import com.gmc.retreat.registration.dto.AdminParticipantChurchCellUpdateRequest;
import com.gmc.retreat.registration.dto.AdminRegistrationResponse;
import com.gmc.retreat.registration.service.RegistrationService;
import com.gmc.retreat.retreat.dto.RetreatGroupAssignmentRequest;
import com.gmc.retreat.retreat.service.RetreatGroupService;
import com.gmc.retreat.security.auth.AdminPrincipal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/participants")
public class AdminParticipantController {

    private final RegistrationService registrationService;
    private final RetreatGroupService retreatGroupService;

    public AdminParticipantController(
            RegistrationService registrationService,
            RetreatGroupService retreatGroupService
    ) {
        this.registrationService = registrationService;
        this.retreatGroupService = retreatGroupService;
    }

    @PatchMapping("/{participantId}/church-cell")
    public ApiResponse<AdminRegistrationResponse> updateChurchCell(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long participantId,
            @Valid @RequestBody AdminParticipantChurchCellUpdateRequest request
    ) {
        return ApiResponse.success(registrationService.updateParticipantChurchCell(admin, participantId, request));
    }

    @PatchMapping("/{participantId}/retreat-group")
    public ApiResponse<AdminRegistrationResponse> assignRetreatGroup(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long participantId,
            @Valid @RequestBody RetreatGroupAssignmentRequest request
    ) {
        return ApiResponse.success(retreatGroupService.assignParticipant(admin, participantId, request));
    }

    @DeleteMapping("/{participantId}/retreat-group")
    public ApiResponse<AdminRegistrationResponse> removeRetreatGroup(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long participantId,
            @Valid @RequestBody DeleteConfirmationRequest request
    ) {
        request.requireConfirmed();
        return ApiResponse.success(retreatGroupService.removeParticipant(admin, participantId));
    }
}

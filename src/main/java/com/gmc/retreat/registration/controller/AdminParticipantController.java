package com.gmc.retreat.registration.controller;

import com.gmc.retreat.api.ApiResponse;
import com.gmc.retreat.registration.dto.AdminParticipantChurchCellUpdateRequest;
import com.gmc.retreat.registration.dto.AdminRegistrationResponse;
import com.gmc.retreat.registration.service.RegistrationService;
import com.gmc.retreat.security.auth.AdminPrincipal;
import jakarta.validation.Valid;
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

    public AdminParticipantController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PatchMapping("/{participantId}/church-cell")
    public ApiResponse<AdminRegistrationResponse> updateChurchCell(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long participantId,
            @Valid @RequestBody AdminParticipantChurchCellUpdateRequest request
    ) {
        return ApiResponse.success(registrationService.updateParticipantChurchCell(admin, participantId, request));
    }
}

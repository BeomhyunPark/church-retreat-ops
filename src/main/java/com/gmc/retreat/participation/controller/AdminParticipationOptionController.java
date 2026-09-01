package com.gmc.retreat.participation.controller;

import com.gmc.retreat.api.ApiResponse;
import com.gmc.retreat.common.dto.ActiveUpdateRequest;
import com.gmc.retreat.participation.dto.ParticipationOptionRequest;
import com.gmc.retreat.participation.dto.ParticipationOptionResponse;
import com.gmc.retreat.participation.service.ParticipationOptionService;
import com.gmc.retreat.security.auth.AdminPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/participation-options")
public class AdminParticipationOptionController {

    private final ParticipationOptionService participationOptionService;

    public AdminParticipationOptionController(ParticipationOptionService participationOptionService) {
        this.participationOptionService = participationOptionService;
    }

    @GetMapping
    public ApiResponse<List<ParticipationOptionResponse>> options(
            @AuthenticationPrincipal AdminPrincipal admin
    ) {
        return ApiResponse.success(participationOptionService.findAdminOptions(admin));
    }

    @PostMapping
    public ApiResponse<ParticipationOptionResponse> create(
            @AuthenticationPrincipal AdminPrincipal admin,
            @Valid @RequestBody ParticipationOptionRequest request
    ) {
        return ApiResponse.success(participationOptionService.create(admin, request));
    }

    @PatchMapping("/{id}")
    public ApiResponse<ParticipationOptionResponse> update(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long id,
            @Valid @RequestBody ParticipationOptionRequest request
    ) {
        return ApiResponse.success(participationOptionService.update(admin, id, request));
    }

    @PatchMapping("/{id}/active")
    public ApiResponse<ParticipationOptionResponse> updateActive(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long id,
            @Valid @RequestBody ActiveUpdateRequest request
    ) {
        return ApiResponse.success(participationOptionService.updateActive(admin, id, request));
    }
}

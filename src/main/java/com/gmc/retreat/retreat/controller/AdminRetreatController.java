package com.gmc.retreat.retreat.controller;

import com.gmc.retreat.api.ApiResponse;
import com.gmc.retreat.retreat.dto.RetreatCreateRequest;
import com.gmc.retreat.retreat.dto.RetreatResponse;
import com.gmc.retreat.retreat.dto.RetreatRegistrationOpenUpdateRequest;
import com.gmc.retreat.retreat.dto.RetreatStatusUpdateRequest;
import com.gmc.retreat.retreat.dto.RetreatUpdateRequest;
import com.gmc.retreat.retreat.service.RetreatService;
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
@RequestMapping("/api/admin/retreats")
public class AdminRetreatController {

    private final RetreatService retreatService;

    public AdminRetreatController(RetreatService retreatService) {
        this.retreatService = retreatService;
    }

    @GetMapping
    public ApiResponse<List<RetreatResponse>> findAll(@AuthenticationPrincipal AdminPrincipal admin) {
        return ApiResponse.success(retreatService.findAll(admin));
    }

    @GetMapping("/current")
    public ApiResponse<RetreatResponse> findCurrent(@AuthenticationPrincipal AdminPrincipal admin) {
        return ApiResponse.success(retreatService.findCurrent(admin));
    }

    @PostMapping
    public ApiResponse<RetreatResponse> create(
            @AuthenticationPrincipal AdminPrincipal admin,
            @Valid @RequestBody RetreatCreateRequest request
    ) {
        return ApiResponse.success(retreatService.create(admin, request));
    }

    @PatchMapping("/{id}")
    public ApiResponse<RetreatResponse> update(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long id,
            @Valid @RequestBody RetreatUpdateRequest request
    ) {
        return ApiResponse.success(retreatService.update(admin, id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<RetreatResponse> updateStatus(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long id,
            @Valid @RequestBody RetreatStatusUpdateRequest request
    ) {
        return ApiResponse.success(retreatService.updateStatus(admin, id, request));
    }

    @PatchMapping("/{id}/registration-open")
    public ApiResponse<RetreatResponse> updateRegistrationOpen(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long id,
            @Valid @RequestBody RetreatRegistrationOpenUpdateRequest request
    ) {
        return ApiResponse.success(retreatService.updateRegistrationOpen(admin, id, request));
    }
}

package com.gmc.retreat.fee.controller;

import com.gmc.retreat.api.ApiResponse;
import com.gmc.retreat.fee.dto.FeeDetailResponse;
import com.gmc.retreat.fee.dto.FeeEventResponse;
import com.gmc.retreat.fee.dto.FeeRosterResponse;
import com.gmc.retreat.fee.dto.FeeStatusUpdateRequest;
import com.gmc.retreat.fee.service.FeeManagementService;
import com.gmc.retreat.registration.dto.PageResponse;
import com.gmc.retreat.security.auth.AdminPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/fees")
public class AdminFeeController {

    private final FeeManagementService feeManagementService;

    public AdminFeeController(FeeManagementService feeManagementService) {
        this.feeManagementService = feeManagementService;
    }

    @GetMapping
    public ApiResponse<PageResponse<FeeRosterResponse>> roster(
            @AuthenticationPrincipal AdminPrincipal admin,
            @RequestParam(required = false) Boolean feePaid,
            @RequestParam(required = false) Long retreatGroupId,
            @RequestParam(required = false) Long churchCellId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(feeManagementService.findRoster(
                admin,
                feePaid,
                retreatGroupId,
                churchCellId,
                keyword,
                page,
                size
        ));
    }

    @GetMapping("/{participantId}")
    public ApiResponse<FeeDetailResponse> detail(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long participantId
    ) {
        return ApiResponse.success(feeManagementService.findDetail(admin, participantId));
    }

    @PatchMapping("/{participantId}")
    public ApiResponse<FeeDetailResponse> update(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long participantId,
            @Valid @RequestBody FeeStatusUpdateRequest request
    ) {
        return ApiResponse.success(feeManagementService.updateFeeStatus(admin, participantId, request));
    }

    @GetMapping("/{participantId}/events")
    public ApiResponse<List<FeeEventResponse>> events(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long participantId
    ) {
        return ApiResponse.success(feeManagementService.findEvents(admin, participantId));
    }
}

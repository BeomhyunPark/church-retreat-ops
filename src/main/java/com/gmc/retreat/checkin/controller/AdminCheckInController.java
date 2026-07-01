package com.gmc.retreat.checkin.controller;

import com.gmc.retreat.api.ApiResponse;
import com.gmc.retreat.checkin.dto.CheckInCancellationRequest;
import com.gmc.retreat.checkin.dto.CheckInRosterResponse;
import com.gmc.retreat.checkin.dto.CheckInQrScanRequest;
import com.gmc.retreat.checkin.dto.CheckInTokenIssueRequest;
import com.gmc.retreat.checkin.dto.CheckInTokenIssueResponse;
import com.gmc.retreat.checkin.dto.CheckInTokenRevokeResponse;
import com.gmc.retreat.checkin.service.CheckInService;
import com.gmc.retreat.registration.dto.PageResponse;
import com.gmc.retreat.security.auth.AdminPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/check-ins")
public class AdminCheckInController {

    private final CheckInService checkInService;

    public AdminCheckInController(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    @GetMapping
    public ApiResponse<PageResponse<CheckInRosterResponse>> roster(
            @AuthenticationPrincipal AdminPrincipal admin,
            @RequestParam(required = false) Boolean checkedIn,
            @RequestParam(required = false) Long retreatGroupId,
            @RequestParam(required = false) Long churchCellId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(checkInService.findRoster(
                admin,
                checkedIn,
                retreatGroupId,
                churchCellId,
                keyword,
                page,
                size
        ));
    }

    @GetMapping("/{participantId}")
    public ApiResponse<CheckInRosterResponse> detail(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long participantId
    ) {
        return ApiResponse.success(checkInService.findDetail(admin, participantId));
    }

    @PostMapping("/{participantId}")
    public ApiResponse<CheckInRosterResponse> manuallyCheckIn(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long participantId
    ) {
        return ApiResponse.success(checkInService.manuallyCheckIn(admin, participantId));
    }

    @PostMapping("/qr")
    public ApiResponse<CheckInRosterResponse> checkInByQr(
            @AuthenticationPrincipal AdminPrincipal admin,
            @Valid @RequestBody CheckInQrScanRequest request
    ) {
        return ApiResponse.success(checkInService.checkInByQr(admin, request));
    }

    @PatchMapping("/{participantId}/cancel")
    public ApiResponse<CheckInRosterResponse> cancelCheckIn(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long participantId,
            @Valid @RequestBody CheckInCancellationRequest request
    ) {
        return ApiResponse.success(checkInService.cancelCheckIn(admin, participantId, request));
    }

    @PostMapping("/tokens/{participantId}")
    public ApiResponse<CheckInTokenIssueResponse> issueToken(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long participantId,
            @Valid @RequestBody CheckInTokenIssueRequest request
    ) {
        return ApiResponse.success(checkInService.issueToken(admin, participantId, request));
    }

    @PatchMapping("/tokens/{participantId}/revoke")
    public ApiResponse<CheckInTokenRevokeResponse> revokeToken(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long participantId
    ) {
        return ApiResponse.success(checkInService.revokeToken(admin, participantId));
    }
}

package com.gmc.retreat.retreat.controller;

import com.gmc.retreat.api.ApiResponse;
import com.gmc.retreat.common.dto.DeleteConfirmationRequest;
import com.gmc.retreat.common.dto.ActiveUpdateRequest;
import com.gmc.retreat.registration.dto.AdminRegistrationResponse;
import com.gmc.retreat.retreat.dto.RetreatGroupLeaderRequest;
import com.gmc.retreat.retreat.dto.RetreatGroupMemberResponse;
import com.gmc.retreat.retreat.dto.RetreatGroupMemberOrderRequest;
import com.gmc.retreat.retreat.dto.RetreatGroupRequest;
import com.gmc.retreat.retreat.dto.RetreatGroupResponse;
import com.gmc.retreat.retreat.dto.RetreatGroupTreeResponse;
import com.gmc.retreat.retreat.service.RetreatGroupService;
import com.gmc.retreat.security.auth.AdminPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/retreat-groups")
public class AdminRetreatGroupController {

    private final RetreatGroupService retreatGroupService;

    public AdminRetreatGroupController(RetreatGroupService retreatGroupService) {
        this.retreatGroupService = retreatGroupService;
    }

    @GetMapping
    public ApiResponse<List<RetreatGroupResponse>> groups(
            @AuthenticationPrincipal AdminPrincipal admin
    ) {
        return ApiResponse.success(retreatGroupService.findGroups(admin));
    }

    @GetMapping("/{id}")
    public ApiResponse<RetreatGroupResponse> group(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long id
    ) {
        return ApiResponse.success(retreatGroupService.findGroup(admin, id));
    }

    @PostMapping
    public ApiResponse<RetreatGroupResponse> createGroup(
            @AuthenticationPrincipal AdminPrincipal admin,
            @Valid @RequestBody RetreatGroupRequest request
    ) {
        return ApiResponse.success(retreatGroupService.createGroup(admin, request));
    }

    @PatchMapping("/{id}")
    public ApiResponse<RetreatGroupResponse> updateGroup(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long id,
            @Valid @RequestBody RetreatGroupRequest request
    ) {
        return ApiResponse.success(retreatGroupService.updateGroup(admin, id, request));
    }

    @PatchMapping("/{id}/active")
    public ApiResponse<RetreatGroupResponse> updateGroupActive(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long id,
            @Valid @RequestBody ActiveUpdateRequest request
    ) {
        return ApiResponse.success(retreatGroupService.updateGroupActive(admin, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteGroup(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long id,
            @Valid @RequestBody DeleteConfirmationRequest request
    ) {
        request.requireConfirmed();
        retreatGroupService.deleteGroup(admin, id);
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}/members")
    public ApiResponse<List<RetreatGroupMemberResponse>> members(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long id
    ) {
        return ApiResponse.success(retreatGroupService.findMembers(admin, id));
    }

    @PatchMapping("/{id}/members/order")
    public ApiResponse<List<RetreatGroupMemberResponse>> updateMemberOrder(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long id,
            @Valid @RequestBody RetreatGroupMemberOrderRequest request
    ) {
        return ApiResponse.success(retreatGroupService.updateMemberOrder(admin, id, request));
    }

    @GetMapping("/tree")
    public ApiResponse<RetreatGroupTreeResponse> tree(
            @AuthenticationPrincipal AdminPrincipal admin
    ) {
        return ApiResponse.success(retreatGroupService.findTree(admin));
    }

    @PatchMapping("/{groupId}/leader")
    public ApiResponse<AdminRegistrationResponse> assignLeader(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long groupId,
            @Valid @RequestBody RetreatGroupLeaderRequest request
    ) {
        return ApiResponse.success(retreatGroupService.assignLeader(admin, groupId, request));
    }

    @DeleteMapping("/{groupId}/leader")
    public ApiResponse<List<RetreatGroupMemberResponse>> removeLeader(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long groupId,
            @Valid @RequestBody DeleteConfirmationRequest request
    ) {
        request.requireConfirmed();
        return ApiResponse.success(retreatGroupService.removeLeader(admin, groupId));
    }
}

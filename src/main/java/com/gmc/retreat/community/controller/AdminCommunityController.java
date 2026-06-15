package com.gmc.retreat.community.controller;

import com.gmc.retreat.api.ApiResponse;
import com.gmc.retreat.community.dto.ActiveUpdateRequest;
import com.gmc.retreat.community.dto.ChurchCellRequest;
import com.gmc.retreat.community.dto.ChurchCellResponse;
import com.gmc.retreat.community.dto.ChurchMiddleGroupRequest;
import com.gmc.retreat.community.dto.ChurchMiddleGroupResponse;
import com.gmc.retreat.community.dto.CommunityTreeResponse;
import com.gmc.retreat.community.service.CommunityService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/community")
public class AdminCommunityController {

    private final CommunityService communityService;

    public AdminCommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping("/middle-groups")
    public ApiResponse<List<ChurchMiddleGroupResponse>> middleGroups(
            @AuthenticationPrincipal AdminPrincipal admin
    ) {
        return ApiResponse.success(communityService.findMiddleGroups(admin));
    }

    @GetMapping("/middle-groups/{id}")
    public ApiResponse<ChurchMiddleGroupResponse> middleGroup(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long id
    ) {
        return ApiResponse.success(communityService.findMiddleGroup(admin, id));
    }

    @PostMapping("/middle-groups")
    public ApiResponse<ChurchMiddleGroupResponse> createMiddleGroup(
            @AuthenticationPrincipal AdminPrincipal admin,
            @Valid @RequestBody ChurchMiddleGroupRequest request
    ) {
        return ApiResponse.success(communityService.createMiddleGroup(admin, request));
    }

    @PatchMapping("/middle-groups/{id}")
    public ApiResponse<ChurchMiddleGroupResponse> updateMiddleGroup(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long id,
            @Valid @RequestBody ChurchMiddleGroupRequest request
    ) {
        return ApiResponse.success(communityService.updateMiddleGroup(admin, id, request));
    }

    @PatchMapping("/middle-groups/{id}/active")
    public ApiResponse<ChurchMiddleGroupResponse> updateMiddleGroupActive(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long id,
            @Valid @RequestBody ActiveUpdateRequest request
    ) {
        return ApiResponse.success(communityService.updateMiddleGroupActive(admin, id, request));
    }

    @GetMapping("/cells")
    public ApiResponse<List<ChurchCellResponse>> cells(
            @AuthenticationPrincipal AdminPrincipal admin,
            @RequestParam(required = false) Long middleGroupId,
            @RequestParam(required = false) Boolean active
    ) {
        return ApiResponse.success(communityService.findCells(admin, middleGroupId, active));
    }

    @GetMapping("/cells/{id}")
    public ApiResponse<ChurchCellResponse> cell(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long id
    ) {
        return ApiResponse.success(communityService.findCell(admin, id));
    }

    @PostMapping("/cells")
    public ApiResponse<ChurchCellResponse> createCell(
            @AuthenticationPrincipal AdminPrincipal admin,
            @Valid @RequestBody ChurchCellRequest request
    ) {
        return ApiResponse.success(communityService.createCell(admin, request));
    }

    @PatchMapping("/cells/{id}")
    public ApiResponse<ChurchCellResponse> updateCell(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long id,
            @Valid @RequestBody ChurchCellRequest request
    ) {
        return ApiResponse.success(communityService.updateCell(admin, id, request));
    }

    @PatchMapping("/cells/{id}/active")
    public ApiResponse<ChurchCellResponse> updateCellActive(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long id,
            @Valid @RequestBody ActiveUpdateRequest request
    ) {
        return ApiResponse.success(communityService.updateCellActive(admin, id, request));
    }

    @GetMapping("/tree")
    public ApiResponse<CommunityTreeResponse> tree(
            @AuthenticationPrincipal AdminPrincipal admin
    ) {
        return ApiResponse.success(communityService.findTree(admin));
    }
}

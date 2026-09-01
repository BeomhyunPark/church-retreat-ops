package com.gmc.retreat.announcement.controller;

import com.gmc.retreat.announcement.dto.AnnouncementRequest;
import com.gmc.retreat.announcement.dto.AnnouncementResponse;
import com.gmc.retreat.announcement.dto.PinnedUpdateRequest;
import com.gmc.retreat.announcement.service.AnnouncementService;
import com.gmc.retreat.api.ApiResponse;
import com.gmc.retreat.common.dto.ActiveUpdateRequest;
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
@RequestMapping("/api/admin/announcements")
public class AdminAnnouncementController {

    private final AnnouncementService announcementService;

    public AdminAnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping
    public ApiResponse<List<AnnouncementResponse>> announcements(
            @AuthenticationPrincipal AdminPrincipal admin
    ) {
        return ApiResponse.success(announcementService.findAnnouncements(admin));
    }

    @GetMapping("/{id}")
    public ApiResponse<AnnouncementResponse> announcement(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long id
    ) {
        return ApiResponse.success(announcementService.findAnnouncement(admin, id));
    }

    @PostMapping
    public ApiResponse<AnnouncementResponse> createAnnouncement(
            @AuthenticationPrincipal AdminPrincipal admin,
            @Valid @RequestBody AnnouncementRequest request
    ) {
        return ApiResponse.success(announcementService.createAnnouncement(admin, request));
    }

    @PatchMapping("/{id}")
    public ApiResponse<AnnouncementResponse> updateAnnouncement(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long id,
            @Valid @RequestBody AnnouncementRequest request
    ) {
        return ApiResponse.success(announcementService.updateAnnouncement(admin, id, request));
    }

    @PatchMapping("/{id}/active")
    public ApiResponse<AnnouncementResponse> updateActive(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long id,
            @Valid @RequestBody ActiveUpdateRequest request
    ) {
        return ApiResponse.success(announcementService.updateActive(admin, id, request));
    }

    @PatchMapping("/{id}/pinned")
    public ApiResponse<AnnouncementResponse> updatePinned(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long id,
            @Valid @RequestBody PinnedUpdateRequest request
    ) {
        return ApiResponse.success(announcementService.updatePinned(admin, id, request));
    }
}

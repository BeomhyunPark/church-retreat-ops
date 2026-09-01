package com.gmc.retreat.schedule.controller;

import com.gmc.retreat.api.ApiResponse;
import com.gmc.retreat.common.dto.ActiveUpdateRequest;
import com.gmc.retreat.schedule.domain.ScheduleCategory;
import com.gmc.retreat.schedule.dto.ScheduleItemRequest;
import com.gmc.retreat.schedule.dto.ScheduleItemResponse;
import com.gmc.retreat.schedule.service.ScheduleItemService;
import com.gmc.retreat.security.auth.AdminPrincipal;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/admin/schedules")
public class AdminScheduleItemController {

    private final ScheduleItemService scheduleItemService;

    public AdminScheduleItemController(ScheduleItemService scheduleItemService) {
        this.scheduleItemService = scheduleItemService;
    }

    @GetMapping
    public ApiResponse<List<ScheduleItemResponse>> schedules(
            @AuthenticationPrincipal AdminPrincipal admin,
            @RequestParam(required = false) Long retreatId,
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate scheduleDate,
            @RequestParam(required = false) ScheduleCategory category,
            @RequestParam(required = false) Boolean active
    ) {
        return ApiResponse.success(scheduleItemService.findScheduleItems(
                admin,
                retreatId,
                scheduleDate,
                category,
                active
        ));
    }

    @GetMapping("/{id}")
    public ApiResponse<ScheduleItemResponse> schedule(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long id
    ) {
        return ApiResponse.success(scheduleItemService.findScheduleItem(admin, id));
    }

    @PostMapping
    public ApiResponse<ScheduleItemResponse> createSchedule(
            @AuthenticationPrincipal AdminPrincipal admin,
            @Valid @RequestBody ScheduleItemRequest request
    ) {
        return ApiResponse.success(scheduleItemService.createScheduleItem(admin, request));
    }

    @PatchMapping("/{id}")
    public ApiResponse<ScheduleItemResponse> updateSchedule(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long id,
            @Valid @RequestBody ScheduleItemRequest request
    ) {
        return ApiResponse.success(scheduleItemService.updateScheduleItem(admin, id, request));
    }

    @PatchMapping("/{id}/active")
    public ApiResponse<ScheduleItemResponse> updateActive(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long id,
            @Valid @RequestBody ActiveUpdateRequest request
    ) {
        return ApiResponse.success(scheduleItemService.updateActive(admin, id, request));
    }
}

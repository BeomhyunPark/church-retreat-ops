package com.gmc.retreat.registration.controller;

import com.gmc.retreat.api.ApiResponse;
import com.gmc.retreat.registration.dto.AdminRegistrationResponse;
import com.gmc.retreat.registration.dto.PageResponse;
import com.gmc.retreat.registration.dto.RegistrationHistoryResponse;
import com.gmc.retreat.registration.service.RegistrationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/registrations")
public class AdminRegistrationController {

    private final RegistrationService registrationService;

    public AdminRegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminRegistrationResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(registrationService.findRegistrations(page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminRegistrationResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(registrationService.findRegistration(id));
    }

    @GetMapping("/{id}/histories")
    public ApiResponse<List<RegistrationHistoryResponse>> histories(@PathVariable Long id) {
        return ApiResponse.success(registrationService.findHistories(id));
    }
}

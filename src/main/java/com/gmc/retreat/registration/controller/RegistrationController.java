package com.gmc.retreat.registration.controller;

import com.gmc.retreat.api.ApiResponse;
import com.gmc.retreat.registration.dto.RegistrationCreateRequest;
import com.gmc.retreat.registration.dto.RegistrationCreateResponse;
import com.gmc.retreat.registration.dto.RegistrationCheckInQrResponse;
import com.gmc.retreat.registration.dto.RegistrationResponse;
import com.gmc.retreat.registration.dto.RegistrationSelfLookupRequest;
import com.gmc.retreat.registration.dto.RegistrationSelfUpdateRequest;
import com.gmc.retreat.registration.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/registrations")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping
    public ApiResponse<RegistrationCreateResponse> create(@Valid @RequestBody RegistrationCreateRequest request) {
        return ApiResponse.success(registrationService.createOrOverwrite(request));
    }

    @PostMapping("/self/lookup")
    public ApiResponse<RegistrationResponse> selfLookup(@Valid @RequestBody RegistrationSelfLookupRequest request) {
        return ApiResponse.success(registrationService.selfLookup(request));
    }

    @PostMapping("/self/check-in-qr")
    public ApiResponse<RegistrationCheckInQrResponse> selfCheckInQr(
            @Valid @RequestBody RegistrationSelfLookupRequest request
    ) {
        return ApiResponse.success(registrationService.selfCheckInQr(request));
    }

    @PutMapping("/self")
    public ApiResponse<RegistrationResponse> selfUpdate(@Valid @RequestBody RegistrationSelfUpdateRequest request) {
        return ApiResponse.success(registrationService.selfUpdate(request));
    }
}

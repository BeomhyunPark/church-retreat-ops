package com.gmc.retreat.identity;

import com.gmc.retreat.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/identity")
public class AppIdentityController {

    private final AppIdentityProperties properties;

    public AppIdentityController(AppIdentityProperties properties) {
        this.properties = properties;
    }

    @GetMapping
    public ApiResponse<AppIdentityResponse> identity() {
        return ApiResponse.success(AppIdentityResponse.from(properties));
    }
}

package com.gmc.retreat.health;

import com.gmc.retreat.api.ApiResponse;
import java.time.OffsetDateTime;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ApiResponse<HealthResponse> health() {
        return ApiResponse.success(new HealthResponse("UP", OffsetDateTime.now()));
    }

    public record HealthResponse(String status, OffsetDateTime checkedAt) {
    }
}

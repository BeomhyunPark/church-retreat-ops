package com.gmc.retreat.identity;

import com.gmc.retreat.api.ApiResponse;
import com.gmc.retreat.retreat.domain.Retreat;
import com.gmc.retreat.retreat.mapper.RetreatMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/identity")
public class AppIdentityController {

    private final AppIdentityProperties properties;
    private final RetreatMapper retreatMapper;

    public AppIdentityController(AppIdentityProperties properties, RetreatMapper retreatMapper) {
        this.properties = properties;
        this.retreatMapper = retreatMapper;
    }

    @GetMapping
    public ApiResponse<AppIdentityResponse> identity() {
        Retreat current = retreatMapper.findCurrent().orElse(null);
        String eventName = current == null ? properties.resolvedEventName() : current.name();
        boolean registrationOpen = current != null && Boolean.TRUE.equals(current.registrationOpen());
        return ApiResponse.success(AppIdentityResponse.from(properties, eventName, registrationOpen));
    }
}

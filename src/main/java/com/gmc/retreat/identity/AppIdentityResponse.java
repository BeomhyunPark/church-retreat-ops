package com.gmc.retreat.identity;

public record AppIdentityResponse(
        String appName,
        String organizationName,
        String eventName
) {

    public static AppIdentityResponse from(AppIdentityProperties properties) {
        return new AppIdentityResponse(
                properties.resolvedAppName(),
                properties.resolvedOrganizationName(),
                properties.resolvedEventName()
        );
    }
}

package com.gmc.retreat.identity;

public record AppIdentityResponse(
        String appName,
        String organizationName,
        String eventName,
        Boolean registrationOpen
) {

    public static AppIdentityResponse from(
            AppIdentityProperties properties,
            String eventName,
            Boolean registrationOpen
    ) {
        return new AppIdentityResponse(
                properties.resolvedAppName(),
                properties.resolvedOrganizationName(),
                eventName,
                registrationOpen
        );
    }
}

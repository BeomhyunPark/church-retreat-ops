package com.gmc.retreat.identity;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.identity")
public class AppIdentityProperties {

    private static final String DEFAULT_APP_NAME = "청년2부 수련회";
    private static final String DEFAULT_ORGANIZATION_NAME = "지구촌교회 드림공동체 청년2부";
    private static final String DEFAULT_EVENT_NAME = "청년2부 수련회";

    private String appName = DEFAULT_APP_NAME;
    private String organizationName = DEFAULT_ORGANIZATION_NAME;
    private String eventName = DEFAULT_EVENT_NAME;

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String resolvedAppName() {
        return normalize(appName, DEFAULT_APP_NAME);
    }

    public String resolvedOrganizationName() {
        return normalize(organizationName, DEFAULT_ORGANIZATION_NAME);
    }

    public String resolvedEventName() {
        return normalize(eventName, DEFAULT_EVENT_NAME);
    }

    private String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}

package com.gmc.retreat.admin.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bootstrap.system-admin")
public record SystemAdminBootstrapProperties(
        String email,
        String password,
        String name
) {
}

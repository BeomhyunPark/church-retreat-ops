package com.gmc.retreat.registration.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.registration")
public record RegistrationProperties(
        boolean selfEditEnabled
) {
}

package com.gmc.retreat.registration.mapper;

public record RegistrationManagementUpdate(
        Long id,
        String adminMemo,
        Boolean newcomer,
        Boolean careTarget
) {
}

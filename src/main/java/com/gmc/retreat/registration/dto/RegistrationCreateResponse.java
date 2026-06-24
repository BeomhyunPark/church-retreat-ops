package com.gmc.retreat.registration.dto;

public record RegistrationCreateResponse(
        ResultType resultType,
        RegistrationResponse registration
) {
    public enum ResultType {
        CREATED,
        OVERWRITTEN
    }
}

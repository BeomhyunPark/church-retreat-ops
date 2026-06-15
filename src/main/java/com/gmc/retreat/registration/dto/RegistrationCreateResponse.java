package com.gmc.retreat.registration.dto;

public record RegistrationCreateResponse(
        ResultType resultType,
        RegistrationResponse registration,
        String lookupKey,
        String lookupKeyNotice
) {
    public enum ResultType {
        CREATED,
        OVERWRITTEN
    }
}

package com.gmc.retreat.registration.dto;

import com.gmc.retreat.checkin.dto.CheckInQrCredentialResponse;

public record RegistrationCreateResponse(
        ResultType resultType,
        RegistrationResponse registration,
        CheckInQrCredentialResponse checkInQr
) {
    public enum ResultType {
        CREATED,
        OVERWRITTEN
    }
}

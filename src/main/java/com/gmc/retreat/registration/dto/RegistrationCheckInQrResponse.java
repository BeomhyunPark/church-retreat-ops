package com.gmc.retreat.registration.dto;

import com.gmc.retreat.checkin.dto.CheckInQrCredentialResponse;

public record RegistrationCheckInQrResponse(
        RegistrationResponse registration,
        CheckInQrCredentialResponse checkInQr
) {
}

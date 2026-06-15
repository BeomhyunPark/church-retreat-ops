package com.gmc.retreat.registration.service;

import com.gmc.retreat.error.BusinessException;
import com.gmc.retreat.error.ErrorCode;

public final class PhoneNumberNormalizer {

    private PhoneNumberNormalizer() {
    }

    public static String normalize(String phoneNumber) {
        if (phoneNumber == null) {
            throw new BusinessException(ErrorCode.INVALID_PHONE_NUMBER);
        }

        String normalized = phoneNumber.replaceAll("[^0-9]", "");
        if (normalized.length() < 10 || normalized.length() > 11) {
            throw new BusinessException(ErrorCode.INVALID_PHONE_NUMBER);
        }

        return normalized;
    }

    public static String lastFour(String normalizedPhoneNumber) {
        if (normalizedPhoneNumber == null || normalizedPhoneNumber.length() < 4) {
            throw new BusinessException(ErrorCode.INVALID_PHONE_NUMBER);
        }
        return normalizedPhoneNumber.substring(normalizedPhoneNumber.length() - 4);
    }

    public static String mask(String normalizedPhoneNumber) {
        if (normalizedPhoneNumber == null || normalizedPhoneNumber.length() <= 4) {
            return "****";
        }
        return normalizedPhoneNumber.substring(0, 3) + "****" + lastFour(normalizedPhoneNumber);
    }
}

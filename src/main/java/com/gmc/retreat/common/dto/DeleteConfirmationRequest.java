package com.gmc.retreat.common.dto;

import com.gmc.retreat.error.BusinessException;
import com.gmc.retreat.error.ErrorCode;
import jakarta.validation.constraints.NotBlank;

public record DeleteConfirmationRequest(
        @NotBlank String confirmText
) {

    private static final String REQUIRED_CONFIRM_TEXT = "DELETE";

    public void requireConfirmed() {
        if (!REQUIRED_CONFIRM_TEXT.equals(confirmText)) {
            throw new BusinessException(ErrorCode.DELETE_CONFIRMATION_MISMATCH);
        }
    }
}

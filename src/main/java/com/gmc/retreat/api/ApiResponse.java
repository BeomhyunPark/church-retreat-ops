package com.gmc.retreat.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gmc.retreat.error.ErrorCode;
import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        ApiError error,
        OffsetDateTime timestamp
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, OffsetDateTime.now());
    }

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return error(errorCode, errorCode.getMessage());
    }

    public static ApiResponse<Void> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(
                false,
                null,
                new ApiError(errorCode.name(), message),
                OffsetDateTime.now()
        );
    }

    public record ApiError(String code, String message) {
    }
}

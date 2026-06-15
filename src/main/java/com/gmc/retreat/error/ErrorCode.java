package com.gmc.retreat.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Invalid request."),
    INVALID_ADMIN_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid email or password."),
    REGISTRATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Registration was not found."),
    REGISTRATION_LOOKUP_FAILED(HttpStatus.UNAUTHORIZED, "Registration lookup failed."),
    REGISTRATION_EDIT_CLOSED(HttpStatus.FORBIDDEN, "Registration self edit is closed."),
    DUPLICATE_REGISTRATION(HttpStatus.CONFLICT, "Active registration already exists."),
    INVALID_PHONE_NUMBER(HttpStatus.BAD_REQUEST, "Invalid phone number."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication is required."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Access is denied."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Resource was not found."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}

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
    COMMUNITY_NOT_FOUND(HttpStatus.NOT_FOUND, "Community resource was not found."),
    DUPLICATE_COMMUNITY_NAME(HttpStatus.CONFLICT, "Community name already exists."),
    RETREAT_GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "Retreat group was not found."),
    DUPLICATE_RETREAT_GROUP_NAME(HttpStatus.CONFLICT, "Retreat group name already exists."),
    DUPLICATE_RETREAT_GROUP_ASSIGNMENT(HttpStatus.CONFLICT, "Participant is already assigned to a retreat group."),
    ANNOUNCEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Announcement was not found."),
    DUPLICATE_ANNOUNCEMENT_TARGET(HttpStatus.CONFLICT, "Announcement target already exists."),
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

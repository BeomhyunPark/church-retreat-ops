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
    SCHEDULE_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "Schedule item was not found."),
    CHECK_IN_ALREADY_COMPLETED(HttpStatus.CONFLICT, "Participant is already checked in."),
    CHECK_IN_NOT_COMPLETED(HttpStatus.CONFLICT, "Participant is not currently checked in."),
    CHECK_IN_CANCELLATION_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "Check-in cancellation reason is required."),
    CHECK_IN_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "Active check-in token was not found."),
    CHECK_IN_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Check-in token is expired."),
    CHECK_IN_TOKEN_REVOKED(HttpStatus.UNAUTHORIZED, "Check-in token is revoked."),
    CHECK_IN_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Check-in token is invalid."),
    FEE_ALREADY_PAID(HttpStatus.CONFLICT, "Participant fee is already marked as paid."),
    FEE_ALREADY_UNPAID(HttpStatus.CONFLICT, "Participant fee is already marked as unpaid."),
    FEE_REVERT_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "Fee revert reason is required."),
    DELETE_CONFIRMATION_MISMATCH(HttpStatus.BAD_REQUEST, "Delete confirmation text does not match."),
    ADMIN_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "Admin user was not found."),
    DUPLICATE_ADMIN_EMAIL(HttpStatus.CONFLICT, "Admin email already exists."),
    INVALID_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "Current password is incorrect."),
    ADMIN_SELF_STATUS_CHANGE_FORBIDDEN(HttpStatus.FORBIDDEN, "Admins cannot change their own account status."),
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

package com.devbridge.logicore.enums;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    SHIPMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "error.shipment.notFound"),
    TRACKING_NOT_FOUND(HttpStatus.NOT_FOUND, "error.tracking.notFound"),
    INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT, "error.shipment.invalidTransition"),
    DUPLICATE_ACTION(HttpStatus.CONFLICT, "error.shipment.duplicateAction"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "error.user.notFound"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "error.user.emailExists"),
    USER_SELF_CHANGE_NOT_ALLOWED(HttpStatus.CONFLICT, "error.user.selfChange"),
    // 400, not 401: a 401 would make the UI think the login expired and log the user out.
    PASSWORD_INCORRECT(HttpStatus.BAD_REQUEST, "error.password.incorrect"),
    PASSWORD_UNCHANGED(HttpStatus.BAD_REQUEST, "error.password.unchanged"),
    CUSTOMER_NOT_FOUND(HttpStatus.NOT_FOUND, "error.customer.notFound"),
    CUSTOMER_NAME_EXISTS(HttpStatus.CONFLICT, "error.customer.nameExists"),
    CUSTOMER_REQUIRED(HttpStatus.BAD_REQUEST, "error.customer.required"),
    CUSTOMER_INACTIVE(HttpStatus.CONFLICT, "error.customer.inactive"),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "error.auth.unauthenticated"),
    BAD_CREDENTIALS(HttpStatus.UNAUTHORIZED, "error.auth.badCredentials"),
    LOGIN_LOCKED(HttpStatus.TOO_MANY_REQUESTS, "error.auth.loginLocked"),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "error.rateLimit.exceeded"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "error.auth.forbidden"),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "error.validation.failed"),
    REQUEST_UNREADABLE(HttpStatus.BAD_REQUEST, "error.request.unreadable"),
    UNEXPECTED(HttpStatus.INTERNAL_SERVER_ERROR, "error.unexpected");

    private final HttpStatus status;
    private final String messageKey;

    ErrorCode(HttpStatus status, String messageKey) {
        this.status = status;
        this.messageKey = messageKey;
    }

    public HttpStatus getStatus() { return status; }
    public String getMessageKey() { return messageKey; }
}

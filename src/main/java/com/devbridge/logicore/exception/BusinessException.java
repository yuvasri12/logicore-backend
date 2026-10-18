package com.devbridge.logicore.exception;

import com.devbridge.logicore.enums.ErrorCode;

public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object[] args;

    public BusinessException(ErrorCode errorCode, Object... args) {
        super(errorCode.name());
        this.errorCode = errorCode;
        this.args = args == null ? new Object[0] : args;
    }

    public ErrorCode getErrorCode() { return errorCode; }
    public Object[] getArgs() { return args; }
}